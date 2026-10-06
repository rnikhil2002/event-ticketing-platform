package com.ticketing.booking.payment;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/**
 * Stand-in for a real provider like Stripe. It behaves like one in the way that matters here:
 * the same idempotency key always returns the same payment instead of charging again.
 */
@Component
public class FakePaymentGateway implements PaymentGateway {

    private final Map<String, String> byKey = new ConcurrentHashMap<>();
    private final AtomicInteger charges = new AtomicInteger();
    private final AtomicInteger refunds = new AtomicInteger();

    @Override
    public String charge(String userId, int amountCents, String idempotencyKey) {
        return byKey.computeIfAbsent(userId + ":" + idempotencyKey, k -> {
            charges.incrementAndGet();
            return "pay_" + UUID.randomUUID();
        });
    }

    @Override
    public void refund(String paymentRef) {
        refunds.incrementAndGet();
    }

    public int chargeCount() { return charges.get(); }
    public int refundCount() { return refunds.get(); }
}
