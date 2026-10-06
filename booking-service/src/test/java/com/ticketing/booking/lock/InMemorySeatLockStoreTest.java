package com.ticketing.booking.lock;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class InMemorySeatLockStoreTest {

    static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    @Test
    void lockIsExclusiveUntilItExpires() {
        MutableClock clock = new MutableClock();
        InMemorySeatLockStore store = new InMemorySeatLockStore(clock);
        assertThat(store.tryLock("s", "a", Duration.ofMinutes(5))).isTrue();
        assertThat(store.tryLock("s", "b", Duration.ofMinutes(5))).isFalse();
        clock.now = clock.now.plus(Duration.ofMinutes(6));
        assertThat(store.tryLock("s", "b", Duration.ofMinutes(5))).isTrue();
    }

    @Test
    void onlyTheOwnerCanRelease() {
        InMemorySeatLockStore store = new InMemorySeatLockStore();
        store.tryLock("s", "a", Duration.ofMinutes(5));
        store.release("s", "b");
        assertThat(store.owners(List.of("s"))).containsEntry("s", "a");
        store.release("s", "a");
        assertThat(store.owners(List.of("s"))).isEmpty();
    }
}
