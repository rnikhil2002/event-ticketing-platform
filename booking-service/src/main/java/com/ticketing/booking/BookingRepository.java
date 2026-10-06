package com.ticketing.booking;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, UUID> {
    Optional<Booking> findByUserIdAndIdempotencyKey(String userId, String idempotencyKey);
    List<Booking> findByUserIdOrderByCreatedAtDesc(String userId);
}
