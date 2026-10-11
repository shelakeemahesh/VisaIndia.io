package com.visasetu.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
@Slf4j
public class JwtTokenService {

    private final SecretKey signingKey;
    private final long tokenExpirationSeconds;

    public JwtTokenService(
            @Value("${visasetu.jwt.secret:VisaSetuEnterpriseSecretKeyForJwtSigningMustBeAtLeast256BitsLong2026}") String secret,
            @Value("${visasetu.jwt.expiration-seconds:86400}") long tokenExpirationSeconds
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.tokenExpirationSeconds = tokenExpirationSeconds;
    }

    public String generateToken(UUID userId, String email, String role) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(tokenExpirationSeconds);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = extractClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            log.debug("JWT token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public String extractEmail(String token) {
        return extractClaims(token).get("email", String.class);
    }

    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(extractClaims(token).getSubject());
    }

    public Date extractIssuedAt(String token) {
        return extractClaims(token).getIssuedAt();
    }

    public long getExpirationSeconds() {
        return tokenExpirationSeconds;
    }
}
