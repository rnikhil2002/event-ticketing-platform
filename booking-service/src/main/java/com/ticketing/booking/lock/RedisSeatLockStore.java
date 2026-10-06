package com.ticketing.booking.lock;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/**
 * Distributed seat locks on Redis, shared by every booking-service instance.
 * Acquire is SET key owner NX PX ttl. Release is a Lua compare-and-delete so it is atomic.
 */
@Component
@ConditionalOnProperty(name = "app.seat-lock.store", havingValue = "redis", matchIfMissing = true)
public class RedisSeatLockStore implements SeatLockStore {

    private static final DefaultRedisScript<Long> RELEASE = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate redis;

    public RedisSeatLockStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean tryLock(String key, String owner, Duration ttl) {
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, owner, ttl));
    }

    @Override
    public void release(String key, String owner) {
        redis.execute(RELEASE, List.of(key), owner);
    }

    @Override
    public Map<String, String> owners(List<String> keys) {
        Map<String, String> out = new HashMap<>();
        if (keys.isEmpty()) return out;
        List<String> values = redis.opsForValue().multiGet(keys);
        if (values == null) return out;
        for (int i = 0; i < keys.size(); i++) {
            if (values.get(i) != null) out.put(keys.get(i), values.get(i));
        }
        return out;
    }
}
