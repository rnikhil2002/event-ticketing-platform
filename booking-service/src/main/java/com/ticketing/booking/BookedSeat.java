package com.ticketing.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

/**
 * One row per sold seat. The unique constraint on (eventId, seatId) is the last line of defense:
 * even if a lock somehow expired at the wrong moment, the database will not sell a seat twice.
 */
@Entity
@Table(name = "booked_seats", uniqueConstraints = @UniqueConstraint(name = "uk_booked_seat", columnNames = {"event_id", "seat_id"}))
public class BookedSeat {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID bookingId;

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(name = "seat_id", nullable = false)
    private String seatId;

    protected BookedSeat() {}

    public BookedSeat(UUID bookingId, String eventId, String seatId) {
        this.id = UUID.randomUUID();
        this.bookingId = bookingId;
        this.eventId = eventId;
        this.seatId = seatId;
    }

    public UUID getBookingId() { return bookingId; }
    public String getEventId() { return eventId; }
    public String getSeatId() { return seatId; }
}
