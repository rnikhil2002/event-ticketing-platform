package com.ticketing.booking.events;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/** Calls event-service to check the event exists and get its seat map and price. */
@Component
public class HttpEventClient implements EventClient {

    private final RestClient http;

    public HttpEventClient(@Value("${app.event-service-url}") String baseUrl) {
        this.http = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public Optional<EventInfo> find(String eventId) {
        try {
            return Optional.ofNullable(http.get().uri("/api/events/{id}", eventId).retrieve().body(EventInfo.class));
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND || e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                return Optional.empty();
            }
            throw e;
        }
    }
}
