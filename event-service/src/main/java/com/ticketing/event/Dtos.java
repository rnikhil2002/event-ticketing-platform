package com.ticketing.event;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class Dtos {
    private Dtos() {}

    public record CreateEventRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 120) String venue,
            @NotNull @Future Instant startsAt,
            @Min(0) @Max(1_000_000) int priceCents,
            @Min(1) @Max(26) int rows,
            @Min(1) @Max(50) int seatsPerRow) {}

    public record EventSummary(String id, String name, String venue, Instant startsAt, int priceCents, int totalSeats) {
        static EventSummary of(Event e) {
            return new EventSummary(e.getId().toString(), e.getName(), e.getVenue(), e.getStartsAt(),
                    e.getPriceCents(), e.getSeatRows() * e.getSeatsPerRow());
        }
    }

    public record EventDetail(String id, String name, String venue, Instant startsAt, int priceCents,
                              int rows, int seatsPerRow, List<String> seats) {
        static EventDetail of(Event e) {
            return new EventDetail(e.getId().toString(), e.getName(), e.getVenue(), e.getStartsAt(),
                    e.getPriceCents(), e.getSeatRows(), e.getSeatsPerRow(), e.seatIds());
        }
    }
}
