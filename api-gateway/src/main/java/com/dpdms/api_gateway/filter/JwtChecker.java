package com.dpdms.api_gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

// Parses and verifies the same JWTs that auth-service issues.
// The gateway and auth-service simply share the same secret property
// (dpdms.jwt.secret), which must be identical in both services.
@Component
public class JwtChecker {

    private static final String ISSUER = "dpdms-auth-service";

    @Value("${dpdms.jwt.secret}")
    private String secret;

    private SecretKey key;

    @PostConstruct
    void init() {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("dpdms.jwt.secret must be at least 32 characters");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // Returns the claims of a valid token, or throws JwtException for a bad/expired one
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(ISSUER)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
