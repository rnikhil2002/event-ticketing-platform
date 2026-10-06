package com.ticketing.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "events")
public class Event {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String venue;

    @Column(nullable = false)
    private Instant startsAt;

    @Column(nullable = false)
    private int priceCents;

    @Column(name = "seat_rows", nullable = false)
    private int seatRows;

    @Column(nullable = false)
    private int seatsPerRow;

    @Column(nullable = false)
    private String createdBy;

    protected Event() {}

    public Event(String name, String venue, Instant startsAt, int priceCents, int seatRows, int seatsPerRow, String createdBy) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.venue = venue;
        this.startsAt = startsAt;
        this.priceCents = priceCents;
        this.seatRows = seatRows;
        this.seatsPerRow = seatsPerRow;
        this.createdBy = createdBy;
    }

    /** Seats are named like a real venue: rows A, B, C... and numbers 1..n. */
    public List<String> seatIds() {
        List<String> seats = new ArrayList<>(seatRows * seatsPerRow);
        for (int r = 0; r < seatRows; r++) {
            char row = (char) ('A' + r);
            for (int n = 1; n <= seatsPerRow; n++) seats.add(row + String.valueOf(n));
        }
        return seats;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getVenue() { return venue; }
    public Instant getStartsAt() { return startsAt; }
    public int getPriceCents() { return priceCents; }
    public int getSeatRows() { return seatRows; }
    public int getSeatsPerRow() { return seatsPerRow; }
    public String getCreatedBy() { return createdBy; }
}
