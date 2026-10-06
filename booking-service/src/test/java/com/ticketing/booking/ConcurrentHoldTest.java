package com.ticketing.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.ticketing.booking.events.EventClient;
import com.ticketing.booking.events.EventInfo;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class ConcurrentHoldTest {

    @Autowired BookingService service;
    @MockBean EventClient events;

    @Test
    void onlyOneOfManyUsersGetsTheSameSeat() throws Exception {
        String eventId = UUID.randomUUID().toString();
        when(events.find(anyString())).thenReturn(Optional.of(new EventInfo(eventId, "Show", 1000, List.of("A1"))));

        int users = 50;
        ExecutorService pool = Executors.newFixedThreadPool(16);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger wins = new AtomicInteger();
        AtomicInteger losses = new AtomicInteger();

        for (int i = 0; i < users; i++) {
            String user = "user-" + i;
            pool.submit(() -> {
                try {
                    start.await();
                    service.hold(user, eventId, List.of("A1"));
                    wins.incrementAndGet();
                } catch (Exception e) {
                    losses.incrementAndGet();
                }
                return null;
            });
        }
        start.countDown(); // release everyone at the same moment
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        assertThat(wins.get()).isEqualTo(1);
        assertThat(losses.get()).isEqualTo(users - 1);
    }
}
