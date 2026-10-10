package com.visasetu.service;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenServiceTest {

    private JwtTokenService jwtTokenService;
    private final String secret = "VisaSetuEnterpriseSecretKeyForJwtSigningMustBeAtLeast256BitsLong2026Test";

    @BeforeEach
    void setUp() {
        jwtTokenService = new JwtTokenService(secret, 3600);
    }

    @Test
    @DisplayName("generateToken produces a valid signed JWT containing claims")
    void generateToken_producesValidJwt() {
        UUID userId = UUID.randomUUID();
        String email = "traveler@example.com";
        String role = "APPLICANT";

        String token = jwtTokenService.generateToken(userId, email, role);

        assertThat(token).isNotBlank();
        assertThat(jwtTokenService.validateToken(token)).isTrue();
        assertThat(jwtTokenService.extractEmail(token)).isEqualTo(email);
        assertThat(jwtTokenService.extractUserId(token)).isEqualTo(userId);
    }

    @Test
    @DisplayName("validateToken returns false for forged or invalid token")
    void validateToken_invalidToken_returnsFalse() {
        assertThat(jwtTokenService.validateToken("invalid.token.structure")).isFalse();
        assertThat(jwtTokenService.validateToken("")).isFalse();
    }
}
