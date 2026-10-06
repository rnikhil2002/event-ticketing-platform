package com.ticketing.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, UUID> {
    List<Event> findByStartsAtAfterOrderByStartsAtAsc(Instant after);
}
