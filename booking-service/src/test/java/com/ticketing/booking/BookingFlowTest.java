package com.ticketing.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketing.booking.events.EventClient;
import com.ticketing.booking.events.EventInfo;
import com.ticketing.booking.payment.FakePaymentGateway;
import com.ticketing.booking.security.JwtService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class BookingFlowTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JwtService jwt;
    @Autowired FakePaymentGateway payments;
    @MockBean EventClient events;

    String eventId;

    @BeforeEach
    void event() {
        // a fresh event per test so seats don't leak between tests
        eventId = UUID.randomUUID().toString();
        when(events.find(anyString())).thenReturn(Optional.empty());
        when(events.find(eventId)).thenReturn(Optional.of(
                new EventInfo(eventId, "Concert", 5000, List.of("A1", "A2", "A3", "B1", "B2", "B3"))));
    }

    String bearer(String user) {
        return "Bearer " + jwt.issue(user, user + "@example.com");
    }

    ResultActions hold(String user, String... seats) throws Exception {
        String body = json.writeValueAsString(new Dtos.HoldRequest(eventId, List.of(seats)));
        return mvc.perform(post("/api/bookings/holds").header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    String holdId(String user, String... seats) throws Exception {
        String res = hold(user, seats).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(res).get("holdId").asText();
    }

    ResultActions confirm(String user, String holdId, String key) throws Exception {
        var req = post("/api/bookings").header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON).content("{\"holdId\":\"" + holdId + "\"}");
        if (key != null) req = req.header("Idempotency-Key", key);
        return mvc.perform(req);
    }

    @Test
    void holdThenConfirmBooksTheSeats() throws Exception {
        String h = holdId("alice", "A2", "A1");
        confirm("alice", h, "key-1")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.seats[0]").value("A1"))
                .andExpect(jsonPath("$.seats[1]").value("A2"))
                .andExpect(jsonPath("$.totalCents").value(10000));

        mvc.perform(get("/api/bookings/events/" + eventId + "/availability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.booked.length()").value(2))
                .andExpect(jsonPath("$.held.length()").value(0));
    }

    @Test
    void secondUserCannotHoldAHeldSeat() throws Exception {
        holdId("alice", "A1");
        hold("bob", "A1").andExpect(status().isConflict());
        mvc.perform(get("/api/bookings/events/" + eventId + "/availability"))
                .andExpect(jsonPath("$.held[0]").value("A1"));
    }

    @Test
    void holdIsAllOrNothing() throws Exception {
        holdId("alice", "A2");
        hold("bob", "A1", "A2").andExpect(status().isConflict());
        // A1 must not stay locked after bob's failed attempt
        holdId("carol", "A1");
    }

    @Test
    void soldSeatCannotBeHeldAgain() throws Exception {
        confirm("alice", holdId("alice", "B1"), "k").andExpect(status().isCreated());
        hold("bob", "B1").andExpect(status().isConflict());
    }

    @Test
    void sameIdempotencyKeyReturnsSameBookingAndChargesOnce() throws Exception {
        int before = payments.chargeCount();
        String h = holdId("alice", "B2");
        String first = confirm("alice", h, "retry-key").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String second = confirm("alice", h, "retry-key").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode a = json.readTree(first), b = json.readTree(second);
        assertThat(a.get("id").asText()).isEqualTo(b.get("id").asText());
        assertThat(payments.chargeCount() - before).isEqualTo(1);
    }

    @Test
    void confirmRequiresIdempotencyKey() throws Exception {
        confirm("alice", holdId("alice", "B3"), null).andExpect(status().isBadRequest());
    }

    @Test
    void cannotConfirmSomeoneElsesHold() throws Exception {
        confirm("mallory", holdId("alice", "A3"), "k").andExpect(status().isNotFound());
    }

    @Test
    void usedHoldCannotBeConfirmedWithANewKey() throws Exception {
        String h = holdId("alice", "A3");
        confirm("alice", h, "k1").andExpect(status().isCreated());
        confirm("alice", h, "k2").andExpect(status().isConflict());
    }

    @Test
    void releasingAHoldFreesTheSeats() throws Exception {
        String h = holdId("alice", "A1");
        mvc.perform(delete("/api/bookings/holds/" + h).header("Authorization", bearer("alice")))
                .andExpect(status().isNoContent());
        holdId("bob", "A1");
    }

    @Test
    void rejectsUnknownSeatsAndEvents() throws Exception {
        hold("alice", "Z9").andExpect(status().isBadRequest());
        String body = json.writeValueAsString(new Dtos.HoldRequest("no-such-event", List.of("A1")));
        mvc.perform(post("/api/bookings/holds").header("Authorization", bearer("alice"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void endpointsNeedLogin() throws Exception {
        mvc.perform(get("/api/bookings/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void myBookingsListsOnlyMine() throws Exception {
        confirm("dave", holdId("dave", "A1"), "k").andExpect(status().isCreated());
        mvc.perform(get("/api/bookings/me").header("Authorization", bearer("dave")))
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/bookings/me").header("Authorization", bearer("erin")))
                .andExpect(jsonPath("$.length()").value(0));
    }
}
