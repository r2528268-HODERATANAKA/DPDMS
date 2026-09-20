package com.dpdms.auth_service.security;

import com.dpdms.auth_service.model.UserAccount;
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
 * Issues and parses the signed JWTs every DPDMS service trusts.
 *
 * TOKEN CONTRACT (stable - the hazard services and the gateway read these claims):
 *   sub     -> username
 *   role    -> WARD_RECORDER | PROVINCIAL_SUPERVISOR | PROVINCIAL_ADMIN
 *   ward    -> concrete ward for ward recorders, absent otherwise
 *   hazard  -> hazard this token is scoped to, absent for provincial admins
 *   name    -> full name (used as the reviewer name on approve/reject)
 *   iss     -> "dpdms-auth-service", plus standard iat/exp
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

    /** Issue a token from an account, carrying its (role, ward, hazard) scope. */
    public String issue(UserAccount user) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .issuer(ISSUER)
                .subject(user.getUsername())
                .claim("role", user.getRole().name())
                .claim("name", user.getFullName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationMinutes * 60)))
                .signWith(key);

        if (user.getWard() != null && !user.getWard().isBlank()) {
            builder.claim("ward", user.getWard());
        }
        if (user.getHazard() != null && !user.getHazard().isBlank()) {
            builder.claim("hazard", user.getHazard());
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

    public long getExpirationMinutes() {
        return expirationMinutes;
    }
}
