package com.ticketing.booking.payment;

public interface PaymentGateway {
    /** Charges the user. The idempotency key is passed through so a retried call never charges twice. */
    String charge(String userId, int amountCents, String idempotencyKey);

    void refund(String paymentRef);
}
