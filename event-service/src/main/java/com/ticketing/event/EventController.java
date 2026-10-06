package com.ticketing.event;

import com.ticketing.event.Dtos.CreateEventRequest;
import com.ticketing.event.Dtos.EventDetail;
import com.ticketing.event.Dtos.EventSummary;
import com.ticketing.event.security.CurrentUser;
import com.ticketing.event.web.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventRepository events;

    public EventController(EventRepository events) {
        this.events = events;
    }

    @GetMapping
    public List<EventSummary> upcoming() {
        return events.findByStartsAtAfterOrderByStartsAtAsc(Instant.now()).stream().map(EventSummary::of).toList();
    }

    @GetMapping("/{id}")
    public EventDetail get(@PathVariable UUID id) {
        return events.findById(id)
                .map(EventDetail::of)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventDetail create(@Valid @RequestBody CreateEventRequest req, HttpServletRequest http) {
        String userId = CurrentUser.require(http);
        Event e = events.save(new Event(req.name(), req.venue(), req.startsAt(), req.priceCents(),
                req.rows(), req.seatsPerRow(), userId));
        return EventDetail.of(e);
    }
}
