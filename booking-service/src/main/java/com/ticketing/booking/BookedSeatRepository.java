package com.ticketing.booking;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookedSeatRepository extends JpaRepository<BookedSeat, UUID> {
    List<BookedSeat> findByEventId(String eventId);
    List<BookedSeat> findByEventIdAndSeatIdIn(String eventId, Collection<String> seatIds);
    List<BookedSeat> findByBookingId(UUID bookingId);
}
