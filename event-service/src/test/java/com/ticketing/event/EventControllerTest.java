package com.ticketing.event;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ticketing.event.security.JwtService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class EventControllerTest {

    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;

    private String body(int rows, int perRow, Instant startsAt) {
        return """
            {"name":"Jazz Night","venue":"Blue Hall","startsAt":"%s","priceCents":4500,"rows":%d,"seatsPerRow":%d}
            """.formatted(startsAt, rows, perRow);
    }

    private String token() {
        return "Bearer " + jwt.issue("organizer-1", "org@example.com");
    }

    @Test
    void createsEventWithSeatMap() throws Exception {
        mvc.perform(post("/api/events").header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(2, 3, Instant.now().plus(10, ChronoUnit.DAYS))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.seats.length()").value(6))
                .andExpect(jsonPath("$.seats[0]").value("A1"))
                .andExpect(jsonPath("$.seats[5]").value("B3"));
    }

    @Test
    void creatingRequiresLogin() throws Exception {
        mvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON)
                        .content(body(2, 3, Instant.now().plus(10, ChronoUnit.DAYS))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsPastDatesAndHugeVenues() throws Exception {
        mvc.perform(post("/api/events").header("Authorization", token()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(2, 3, Instant.now().minus(1, ChronoUnit.DAYS))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/events").header("Authorization", token()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(27, 3, Instant.now().plus(1, ChronoUnit.DAYS))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownEventIs404() throws Exception {
        mvc.perform(get("/api/events/00000000-0000-0000-0000-000000000000")).andExpect(status().isNotFound());
    }

    @Test
    void listsUpcomingEvents() throws Exception {
        mvc.perform(post("/api/events").header("Authorization", token()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(1, 1, Instant.now().plus(5, ChronoUnit.DAYS))))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/events")).andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("Jazz Night"));
    }

    @Test
    void malformedIdIs400() throws Exception {
        mvc.perform(get("/api/events/not-a-uuid")).andExpect(status().isBadRequest());
    }
}
