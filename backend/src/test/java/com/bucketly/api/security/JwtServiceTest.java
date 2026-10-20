package com.bucketly.api.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;
    private String validBase64Secret;

    @BeforeEach
    void setUp() {
        // Generate a cryptographically secure 256-bit key dynamically in memory for testing
        // Ensures zero hardcoded secrets exist in test files or source control.
        SecretKey dynamicKey = Jwts.SIG.HS256.key().build();
        validBase64Secret = Encoders.BASE64.encode(dynamicKey.getEncoded());
        jwtService = new JwtService(validBase64Secret, 86400000L); // 24h expiration
    }

    @Test
    @DisplayName("generateToken creates a valid JWT and validateToken returns true")
    void generateAndValidateToken_success() {
        String token = jwtService.generateToken(42L, "traveler@bucketly.local");

        assertThat(token).isNotBlank();
        assertThat(jwtService.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("extractEmail and extractUserId return correct claims")
    void extractClaims_returnsExpectedValues() {
        String token = jwtService.generateToken(101L, "explorer@bucketly.local");

        assertThat(jwtService.extractEmail(token)).isEqualTo("explorer@bucketly.local");
        assertThat(jwtService.extractUserId(token)).isEqualTo(101L);
    }

    @Test
    @DisplayName("Expired token is rejected by validateToken and throws ExpiredJwtException on parse")
    void expiredToken_isRejected() {
        // Create service with negative expiration (already expired)
        JwtService expiredService = new JwtService(validBase64Secret, -60000L);
        String expiredToken = expiredService.generateToken(1L, "past@bucketly.local");

        assertThat(jwtService.validateToken(expiredToken)).isFalse();
        assertThatThrownBy(() -> jwtService.parseClaims(expiredToken))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    @Test
    @DisplayName("Malformed token is rejected by validateToken")
    void malformedToken_isRejected() {
        String malformedToken = "not.a.valid.jwt.token";

        assertThat(jwtService.validateToken(malformedToken)).isFalse();
        assertThat(jwtService.validateToken("")).isFalse();
        assertThat(jwtService.validateToken(null)).isFalse();
    }

    @Test
    @DisplayName("Token signed with a different key is rejected")
    void differentSigningKey_isRejected() {
        // Generate another distinct key
        SecretKey otherKey = Jwts.SIG.HS256.key().build();
        String otherSecret = Encoders.BASE64.encode(otherKey.getEncoded());
        JwtService otherService = new JwtService(otherSecret, 86400000L);

        String tokenFromOther = otherService.generateToken(50L, "other@bucketly.local");

        // Validating with original service key must fail
        assertThat(jwtService.validateToken(tokenFromOther)).isFalse();
        assertThatThrownBy(() -> jwtService.parseClaims(tokenFromOther))
                .isInstanceOf(io.jsonwebtoken.security.SignatureException.class);
    }

    @Test
    @DisplayName("Unconfigured JwtService throws IllegalStateException on token generation and claim parsing")
    void unconfiguredService_throwsIllegalStateException() {
        JwtService unconfigured = new JwtService("", 86400000L);

        assertThat(unconfigured.isConfigured()).isFalse();
        assertThat(unconfigured.validateToken("any.token.here")).isFalse();

        assertThatThrownBy(() -> unconfigured.generateToken(1L, "user@bucketly.local"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BUCKETLY_JWT_SECRET");

        assertThatThrownBy(() -> unconfigured.parseClaims("any.token.here"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BUCKETLY_JWT_SECRET");
    }

    @Test
    @DisplayName("Weak secret (under 32 bytes) is rejected at initialization with IllegalArgumentException")
    void weakSecret_throwsIllegalArgumentException() {
        String weakSecret = "short-secret-under-32-bytes";

        assertThatThrownBy(() -> new JwtService(weakSecret, 86400000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 256 bits");
    }
}

