package com.ticketing.booking;

import com.ticketing.booking.Dtos.Availability;
import com.ticketing.booking.Dtos.BookingView;
import com.ticketing.booking.Dtos.HoldView;
import com.ticketing.booking.events.EventClient;
import com.ticketing.booking.events.EventInfo;
import com.ticketing.booking.lock.SeatLockStore;
import com.ticketing.booking.payment.PaymentGateway;
import com.ticketing.booking.web.ApiException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Booking flow:
 *   1. hold    - lock the chosen seats in Redis for a few minutes (all or nothing)
 *   2. confirm - charge the card with an idempotency key, then save the booking
 *
 * Double booking is blocked twice: by the Redis lock while the user is checking out,
 * and by a unique (event, seat) constraint in the database when the booking is saved.
 */
@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final SeatLockStore locks;
    private final EventClient events;
    private final PaymentGateway payments;
    private final SeatHoldRepository holds;
    private final BookingRepository bookings;
    private final BookedSeatRepository bookedSeats;
    private final TransactionTemplate tx;
    private final Duration holdTtl;

    public BookingService(SeatLockStore locks, EventClient events, PaymentGateway payments, SeatHoldRepository holds,
                          BookingRepository bookings, BookedSeatRepository bookedSeats, TransactionTemplate tx,
                          @Value("${app.hold-minutes:5}") long holdMinutes) {
        this.locks = locks;
        this.events = events;
        this.payments = payments;
        this.holds = holds;
        this.bookings = bookings;
        this.bookedSeats = bookedSeats;
        this.tx = tx;
        this.holdTtl = Duration.ofMinutes(holdMinutes);
    }

    static String lockKey(String eventId, String seat) {
        return "seat:" + eventId + ":" + seat;
    }

    public HoldView hold(String userId, String eventId, List<String> requested) {
        EventInfo event = events.find(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        List<String> seats = requested.stream().distinct().sorted().toList();
        Set<String> valid = new HashSet<>(event.seats());
        for (String s : seats) {
            if (!valid.contains(s)) throw new ApiException(HttpStatus.BAD_REQUEST, "Seat " + s + " does not exist");
        }
        if (!bookedSeats.findByEventIdAndSeatIdIn(eventId, seats).isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "One or more seats are already sold");
        }

        UUID holdId = UUID.randomUUID();
        String owner = holdId.toString();
        List<String> acquired = new ArrayList<>();
        // Seats are locked in sorted order so two users grabbing overlapping seats can't deadlock-retry forever.
        for (String seat : seats) {
            if (locks.tryLock(lockKey(eventId, seat), owner, holdTtl)) {
                acquired.add(seat);
            } else {
                acquired.forEach(s -> locks.release(lockKey(eventId, s), owner));
                throw new ApiException(HttpStatus.CONFLICT, "Seat " + seat + " was just taken by someone else");
            }
        }

        SeatHold hold = holds.save(new SeatHold(holdId, userId, eventId, seats,
                event.priceCents() * seats.size(), Instant.now().plus(holdTtl)));
        log.info("hold created hold={} event={} seats={}", holdId, eventId, seats);
        return HoldView.of(hold);
    }

    public void release(String userId, UUID holdId) {
        SeatHold hold = ownHold(userId, holdId);
        if (hold.getStatus() != SeatHold.Status.ACTIVE) return;
        hold.seatList().forEach(s -> locks.release(lockKey(hold.getEventId(), s), holdId.toString()));
        hold.setStatus(SeatHold.Status.RELEASED);
        holds.save(hold);
    }

    public BookingView confirm(String userId, UUID holdId, String idempotencyKey) {
        // Same key again (double click, client retry after a timeout): return the original booking.
        var existing = bookings.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
        if (existing.isPresent()) return view(existing.get());

        SeatHold hold = ownHold(userId, holdId);
        if (hold.getStatus() != SeatHold.Status.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, "This hold was already used or released");
        }
        String owner = holdId.toString();
        List<String> keys = hold.seatList().stream().map(s -> lockKey(hold.getEventId(), s)).toList();
        Map<String, String> owners = locks.owners(keys);
        boolean stillOurs = keys.stream().allMatch(k -> owner.equals(owners.get(k)));
        if (!stillOurs || hold.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.CONFLICT, "Your hold expired. Please pick your seats again.");
        }

        String paymentRef = payments.charge(userId, hold.getTotalCents(), idempotencyKey);
        Booking booking;
        try {
            booking = tx.execute(status -> {
                Booking b = bookings.save(new Booking(userId, hold.getEventId(), holdId, hold.getTotalCents(),
                        paymentRef, idempotencyKey));
                for (String seat : hold.seatList()) bookedSeats.save(new BookedSeat(b.getId(), hold.getEventId(), seat));
                hold.setStatus(SeatHold.Status.CONFIRMED);
                holds.save(hold);
                bookings.flush();
                bookedSeats.flush();
                return b;
            });
        } catch (DataIntegrityViolationException e) {
            // Either the same idempotency key raced us, or a seat was sold. Check which.
            var raced = bookings.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
            if (raced.isPresent()) return view(raced.get());
            payments.refund(paymentRef);
            throw new ApiException(HttpStatus.CONFLICT, "A seat in your hold was sold. You have not been charged.");
        }

        keys.forEach(k -> locks.release(k, owner));
        log.info("booking confirmed booking={} hold={} payment={}", booking.getId(), holdId, paymentRef);
        return view(booking);
    }

    public List<BookingView> myBookings(String userId) {
        return bookings.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::view).toList();
    }

    public Availability availability(String eventId) {
        EventInfo event = events.find(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));
        List<String> booked = bookedSeats.findByEventId(eventId).stream().map(BookedSeat::getSeatId).sorted().toList();
        Set<String> bookedSet = new HashSet<>(booked);
        List<String> keys = event.seats().stream().map(s -> lockKey(eventId, s)).toList();
        Map<String, String> owners = locks.owners(keys);
        List<String> held = event.seats().stream()
                .filter(s -> owners.containsKey(lockKey(eventId, s)) && !bookedSet.contains(s))
                .toList();
        return new Availability(eventId, booked, held);
    }

    private SeatHold ownHold(String userId, UUID holdId) {
        return holds.findById(holdId)
                .filter(h -> h.getUserId().equals(userId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Hold not found"));
    }

    private BookingView view(Booking b) {
        List<String> seats = bookedSeats.findByBookingId(b.getId()).stream().map(BookedSeat::getSeatId).sorted().toList();
        return BookingView.of(b, seats);
    }
}
