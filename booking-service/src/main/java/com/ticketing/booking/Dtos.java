package com.ticketing.booking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class Dtos {
    private Dtos() {}

    public record HoldRequest(@NotBlank String eventId, @NotEmpty @Size(max = 10) List<@NotBlank String> seats) {}

    public record ConfirmRequest(@NotNull UUID holdId) {}

    public record HoldView(String holdId, String eventId, List<String> seats, int totalCents, Instant expiresAt) {
        static HoldView of(SeatHold h) {
            return new HoldView(h.getId().toString(), h.getEventId(), h.seatList(), h.getTotalCents(), h.getExpiresAt());
        }
    }

    public record BookingView(String id, String eventId, List<String> seats, int totalCents, String paymentRef,
                              Instant createdAt) {
        static BookingView of(Booking b, List<String> seats) {
            return new BookingView(b.getId().toString(), b.getEventId(), seats, b.getTotalCents(), b.getPaymentRef(),
                    b.getCreatedAt());
        }
    }

    public record Availability(String eventId, List<String> booked, List<String> held) {}
}
