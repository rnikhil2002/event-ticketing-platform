package com.ticketing.user.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads the Bearer token and stores the user id on the request.
 * Controllers decide which endpoints need a user by calling {@link CurrentUser#require}.
 */
@Component
public class AuthFilter extends OncePerRequestFilter {

    public static final String USER_ID = "userId";
    private final JwtService jwt;

    public AuthFilter(JwtService jwt) {
        this.jwt = jwt;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            jwt.verify(header.substring(7)).ifPresent(id -> req.setAttribute(USER_ID, id));
        }
        chain.doFilter(req, res);
    }
}
