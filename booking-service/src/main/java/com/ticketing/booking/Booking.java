package com.ticketing.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bookings", uniqueConstraints = @UniqueConstraint(name = "uk_booking_user_idem", columnNames = {"user_id", "idempotency_key"}))
public class Booking {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private String eventId;

    @Column(nullable = false)
    private UUID holdId;

    @Column(nullable = false)
    private int totalCents;

    @Column(nullable = false)
    private String paymentRef;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(nullable = false)
    private Instant createdAt;

    protected Booking() {}

    public Booking(String userId, String eventId, UUID holdId, int totalCents, String paymentRef, String idempotencyKey) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.eventId = eventId;
        this.holdId = holdId;
        this.totalCents = totalCents;
        this.paymentRef = paymentRef;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getUserId() { return userId; }
    public String getEventId() { return eventId; }
    public UUID getHoldId() { return holdId; }
    public int getTotalCents() { return totalCents; }
    public String getPaymentRef() { return paymentRef; }
    public Instant getCreatedAt() { return createdAt; }
}
