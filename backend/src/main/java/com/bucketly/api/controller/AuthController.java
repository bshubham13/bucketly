package com.bucketly.api.controller;

import com.bucketly.api.dto.AuthResponse;
import com.bucketly.api.dto.LoginRequest;
import com.bucketly.api.dto.RegisterRequest;
import com.bucketly.api.exception.InvalidCredentialsException;
import com.bucketly.api.security.JwtAuthenticationFilter;
import com.bucketly.api.security.JwtService;
import com.bucketly.api.security.UserPrincipal;
import com.bucketly.api.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final boolean cookieSecure;
    private final long jwtExpirationMs;

    public AuthController(
            AuthService authService,
            JwtService jwtService,
            @Value("${bucketly.cookie.secure:${COOKIE_SECURE:false}}") boolean cookieSecure,
            @Value("${bucketly.jwt.expiration-ms:86400000}") long jwtExpirationMs) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.cookieSecure = cookieSecure;
        this.jwtExpirationMs = jwtExpirationMs;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        AuthResponse response = authService.login(request);
        String token = jwtService.generateToken(response.id(), response.email());

        ResponseCookie authCookie = buildAuthCookie(token, httpRequest);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookie.toString())
                .body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest httpRequest) {
        ResponseCookie clearCookie = buildClearCookie(httpRequest);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .body(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null || principal.userId() == null) {
            throw new InvalidCredentialsException("Authentication required.");
        }
        AuthResponse response = authService.getUserById(principal.userId());
        return ResponseEntity.ok(response);
    }

    private ResponseCookie buildAuthCookie(String token, HttpServletRequest request) {
        boolean secure = cookieSecure || (request != null && request.isSecure());
        long maxAgeSeconds = jwtExpirationMs > 0 ? (jwtExpirationMs / 1000) : 86400;

        return ResponseCookie.from(JwtAuthenticationFilter.COOKIE_NAME, token)
                .httpOnly(true)
                .path("/")
                .sameSite("Lax")
                .secure(secure)
                .maxAge(maxAgeSeconds)
                .build();
    }

    private ResponseCookie buildClearCookie(HttpServletRequest request) {
        boolean secure = cookieSecure || (request != null && request.isSecure());

        return ResponseCookie.from(JwtAuthenticationFilter.COOKIE_NAME, "")
                .httpOnly(true)
                .path("/")
                .sameSite("Lax")
                .secure(secure)
                .maxAge(0)
                .build();
    }
}

