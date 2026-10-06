package com.ticketing.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "seat_holds")
public class SeatHold {

    public enum Status { ACTIVE, CONFIRMED, RELEASED }

    @Id
    private UUID id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String eventId;

    @Column(nullable = false, length = 1000)
    private String seats; // comma separated, e.g. "A1,A2"

    @Column(nullable = false)
    private int totalCents;

    @Column(nullable = false)
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    protected SeatHold() {}

    public SeatHold(UUID id, String userId, String eventId, List<String> seats, int totalCents, Instant expiresAt) {
        this.id = id;
        this.userId = userId;
        this.eventId = eventId;
        this.seats = String.join(",", seats);
        this.totalCents = totalCents;
        this.expiresAt = expiresAt;
        this.status = Status.ACTIVE;
    }

    public List<String> seatList() {
        return Arrays.asList(seats.split(","));
    }

    public UUID getId() { return id; }
    public String getUserId() { return userId; }
    public String getEventId() { return eventId; }
    public int getTotalCents() { return totalCents; }
    public Instant getExpiresAt() { return expiresAt; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}
