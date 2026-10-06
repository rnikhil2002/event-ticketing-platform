package com.ticketing.user;

import com.ticketing.user.Dtos.AuthResponse;
import com.ticketing.user.Dtos.LoginRequest;
import com.ticketing.user.Dtos.RegisterRequest;
import com.ticketing.user.Dtos.UserView;
import com.ticketing.user.security.CurrentUser;
import com.ticketing.user.security.JwtService;
import com.ticketing.user.web.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository users;
    private final JwtService jwt;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public UserController(UserRepository users, JwtService jwt) {
        this.users = users;
        this.jwt = jwt;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        String email = req.email().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        }
        User user = users.save(new User(email, req.name(), encoder.encode(req.password())));
        return new AuthResponse(jwt.issue(user.getId().toString(), user.getEmail()), UserView.of(user));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        User user = users.findByEmail(req.email().toLowerCase(Locale.ROOT))
                .filter(u -> encoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Wrong email or password"));
        return new AuthResponse(jwt.issue(user.getId().toString(), user.getEmail()), UserView.of(user));
    }

    @GetMapping("/me")
    public UserView me(HttpServletRequest request) {
        UUID id = UUID.fromString(CurrentUser.require(request));
        return users.findById(id)
                .map(UserView::of)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User no longer exists"));
    }
}
