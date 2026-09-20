package com.dpdms.fire_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Validates the HS256 JWTs issued by auth-service. fire-service normally only parses
 * tokens, but {@link #issue} is kept so tests can mint tokens with the shared secret.
 *
 * Token claims: sub (username), role, ward (optional), hazard (optional), name, iss, iat/exp.
 */
@Component
public class JwtService {

    public static final String ISSUER = "dpdms-auth-service";

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(
            @Value("${dpdms.jwt.secret}") String secret,
            @Value("${dpdms.jwt.expiration-minutes:720}") long expirationMinutes) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("dpdms.jwt.secret must be at least 32 characters for HS256");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    /** Issue a token with the RBAC scope (used by tests and by auth-service's contract). */
    public String issue(String username, String fullName, String role, String ward, String hazard) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .issuer(ISSUER)
                .subject(username)
                .claim("role", role)
                .claim("name", fullName)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationMinutes * 60)))
                .signWith(key);
        if (ward != null && !ward.isBlank()) {
            builder.claim("ward", ward);
        }
        if (hazard != null && !hazard.isBlank()) {
            builder.claim("hazard", hazard);
        }
        return builder.compact();
    }

    /** @throws JwtException when the token is invalid or expired. */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(ISSUER)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
