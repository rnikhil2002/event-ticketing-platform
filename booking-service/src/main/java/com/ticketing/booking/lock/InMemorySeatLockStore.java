package com.ticketing.booking.lock;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Same behaviour as the Redis store, for tests and running a single instance without Redis. */
@Component
@ConditionalOnProperty(name = "app.seat-lock.store", havingValue = "memory")
public class InMemorySeatLockStore implements SeatLockStore {

    private record Entry(String owner, Instant expiresAt) {}

    private final Map<String, Entry> locks = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemorySeatLockStore() {
        this(Clock.systemUTC());
    }

    public InMemorySeatLockStore(Clock clock) {
        this.clock = clock;
    }

    @Override
    public boolean tryLock(String key, String owner, Duration ttl) {
        Instant now = clock.instant();
        Entry fresh = new Entry(owner, now.plus(ttl));
        // compute() runs atomically per key, which gives us the same guarantee as Redis SET NX
        Entry result = locks.compute(key, (k, cur) -> (cur == null || cur.expiresAt().isBefore(now)) ? fresh : cur);
        return result == fresh;
    }

    @Override
    public void release(String key, String owner) {
        locks.computeIfPresent(key, (k, cur) -> cur.owner().equals(owner) ? null : cur);
    }

    @Override
    public Map<String, String> owners(List<String> keys) {
        Instant now = clock.instant();
        Map<String, String> out = new HashMap<>();
        for (String k : keys) {
            Entry e = locks.get(k);
            if (e != null && e.expiresAt().isAfter(now)) out.put(k, e.owner());
        }
        return out;
    }
}
