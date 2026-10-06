package com.ticketing.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class Dtos {
    private Dtos() {}

    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(max = 80) String name,
            @NotBlank @Size(min = 8, max = 128) String password) {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    public record UserView(String id, String email, String name) {
        static UserView of(User u) {
            return new UserView(u.getId().toString(), u.getEmail(), u.getName());
        }
    }

    public record AuthResponse(String token, UserView user) {}
}
