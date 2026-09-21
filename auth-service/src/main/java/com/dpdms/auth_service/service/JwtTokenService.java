package com.dpdms.auth_service.service;

import com.dpdms.auth_service.model.UserAccount;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

// Issues and verifies the DPDMS JWTs (HS256 signed with the shared secret).
//
// TOKEN CONTRACT (also documented in the README):
//   sub    = username
//   role   = WARD_RECORDER | PROVINCIAL_SUPERVISOR | PROVINCIAL_ADMIN
//   name   = full name (becomes "reviewedBy" on approvals)
//   ward   = the recorder's ward            (recorders only)
//   hazard = the hazard this account serves (recorders + supervisors)
//   iss    = dpdms-auth-service, plus iat/exp (issued-at / expiry)
//
// The gateway verifies the token and forwards these claims as X-User-* headers
// to the hazard services, so hazard services never need the JWT library.
@Service
public class JwtTokenService {

    public static final String ISSUER = "dpdms-auth-service";

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtTokenService(@Value("${dpdms.jwt.secret}") String secret,
                           @Value("${dpdms.jwt.expiration-minutes:480}") long expirationMinutes) {
        // A too-short secret would make HS256 weak - fail fast at startup instead.
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "dpdms.jwt.secret must be at least 32 characters long");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    // Build a signed token for a user. ward/hazard claims are only added when present,
    // so admins get a clean token without scope claims.
    public String issue(UserAccount user) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .issuer(ISSUER)
                .subject(user.getUsername())
                .claim("role", user.getRole().name())
                .claim("name", user.getFullName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
                .signWith(key, Jwts.SIG.HS256);
        if (user.getWard() != null && !user.getWard().isBlank()) {
            builder.claim("ward", user.getWard());
        }
        if (user.getHazard() != null && !user.getHazard().isBlank()) {
            builder.claim("hazard", user.getHazard());
        }
        return builder.compact();
    }

    // Verify signature + issuer + expiry, and return the claims.
    // Throws JwtException (handled as HTTP 401 by GlobalExceptionHandler) when invalid.
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(ISSUER)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
