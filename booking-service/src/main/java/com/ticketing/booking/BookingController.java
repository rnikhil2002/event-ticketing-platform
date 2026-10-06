package com.ticketing.booking;

import com.ticketing.booking.Dtos.Availability;
import com.ticketing.booking.Dtos.BookingView;
import com.ticketing.booking.Dtos.ConfirmRequest;
import com.ticketing.booking.Dtos.HoldRequest;
import com.ticketing.booking.Dtos.HoldView;
import com.ticketing.booking.security.CurrentUser;
import com.ticketing.booking.web.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService service;

    public BookingController(BookingService service) {
        this.service = service;
    }

    @PostMapping("/holds")
    @ResponseStatus(HttpStatus.CREATED)
    public HoldView hold(@Valid @RequestBody HoldRequest req, HttpServletRequest http) {
        return service.hold(CurrentUser.require(http), req.eventId(), req.seats());
    }

    @DeleteMapping("/holds/{holdId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void release(@PathVariable UUID holdId, HttpServletRequest http) {
        service.release(CurrentUser.require(http), holdId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingView confirm(@Valid @RequestBody ConfirmRequest req,
                               @RequestHeader(name = "Idempotency-Key", required = false) String key,
                               HttpServletRequest http) {
        String userId = CurrentUser.require(http);
        if (key == null || key.isBlank() || key.length() > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Idempotency-Key header is required");
        }
        return service.confirm(userId, req.holdId(), key);
    }

    @GetMapping("/me")
    public List<BookingView> mine(HttpServletRequest http) {
        return service.myBookings(CurrentUser.require(http));
    }

    @GetMapping("/events/{eventId}/availability")
    public Availability availability(@PathVariable String eventId) {
        return service.availability(eventId);
    }
}
