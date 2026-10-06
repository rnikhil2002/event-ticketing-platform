package com.ticketing.event.security;

import com.ticketing.event.web.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;

public final class CurrentUser {
    private CurrentUser() {}

    public static String require(HttpServletRequest req) {
        Object id = req.getAttribute(AuthFilter.USER_ID);
        if (id == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "Login required");
        return id.toString();
    }
}
