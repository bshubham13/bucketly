package com.bucketly.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final long expirationMs;
    private final SecretKey signingKey;

    public JwtService(
            @Value("${bucketly.jwt.secret:}") String secret,
            @Value("${bucketly.jwt.expiration-ms:86400000}") long expirationMs) {
        this.expirationMs = expirationMs;
        this.signingKey = initSigningKey(secret != null ? secret.trim() : "");
    }

    private SecretKey initSigningKey(String secret) {
        if (secret.isBlank()) {
            return null;
        }
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (Exception e) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret key must be at least 256 bits (32 bytes). Current length: " + keyBytes.length + " bytes.");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public boolean isConfigured() {
        return this.signingKey != null;
    }

    public String generateToken(Long userId, String email) {
        assertConfigured();
        if (userId == null || email == null || email.isBlank()) {
            throw new IllegalArgumentException("userId and email are required to generate JWT");
        }
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(email.trim().toLowerCase())
                .claim("userId", userId)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        if (!isConfigured() || token == null || token.isBlank()) {
            return false;
        }
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        Claims claims = parseClaims(token);
        Object userIdObj = claims.get("userId");
        if (userIdObj instanceof Number number) {
            return number.longValue();
        } else if (userIdObj instanceof String str) {
            return Long.parseLong(str);
        }
        return null;
    }

    public Claims parseClaims(String token) {
        assertConfigured();
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token.trim())
                .getPayload();
    }

    private void assertConfigured() {
        if (this.signingKey == null) {
            throw new IllegalStateException("JWT signing key is not configured. Please supply a valid BUCKETLY_JWT_SECRET environment variable.");
        }
    }
}

