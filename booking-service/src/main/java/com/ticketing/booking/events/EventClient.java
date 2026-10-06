package com.ticketing.booking.events;

import java.util.Optional;

public interface EventClient {
    Optional<EventInfo> find(String eventId);
}
