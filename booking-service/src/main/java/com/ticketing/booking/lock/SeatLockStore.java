package com.ticketing.booking.lock;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/** A short-lived lock per seat. The lock value is the id of the hold that owns it. */
public interface SeatLockStore {

    /** Takes the lock only if nobody holds it. Atomic. */
    boolean tryLock(String key, String owner, Duration ttl);

    /** Releases the lock only if {@code owner} still holds it, so an expired hold can't free someone else's seat. */
    void release(String key, String owner);

    /** Current owner for each key that is locked. Missing keys are free. */
    Map<String, String> owners(List<String> keys);
}
