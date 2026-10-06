package com.ticketing.booking.events;

import java.util.List;

public record EventInfo(String id, String name, int priceCents, List<String> seats) {}
