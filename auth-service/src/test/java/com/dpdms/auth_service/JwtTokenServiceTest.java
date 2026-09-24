package com.dpdms.auth_service;

import com.dpdms.auth_service.model.Role;
import com.dpdms.auth_service.model.UserAccount;
import com.dpdms.auth_service.service.JwtTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

// Unit tests for JWT issuing and parsing. No Spring context, no database.
class JwtTokenServiceTest {

    private static final String SECRET =
            "dpdms-test-secret-key-which-is-long-enough-0123456789abcdef";

    private JwtTokenService service;

    @BeforeEach
    void setUp() {
        service = new JwtTokenService(SECRET, 480);
    }

    private UserAccount recorder() {
        return UserAccount.builder()
                .id(1L).username("tanaka").fullName("Tanaka M.")
                .role(Role.WARD_RECORDER)
                .ward("Mudzi").hazard("flood").active(true)
                .build();
    }

    @Test
    void issuedTokenCanBeParsedBackWithAllClaims() {
        String token = service.issue(recorder());
        Claims claims = service.parse(token);

        assertEquals("tanaka", claims.getSubject());
        assertEquals("WARD_RECORDER", claims.get("role", String.class));
        assertEquals("Tanaka M.", claims.get("name", String.class));
        assertEquals("Mudzi", claims.get("ward", String.class));
        assertEquals("flood", claims.get("hazard", String.class));
        assertEquals("dpdms-auth-service", claims.getIssuer());
    }

    @Test
    void adminTokenHasNoWardOrHazardClaims() {
        UserAccount admin = UserAccount.builder()
                .id(2L).username("boss").fullName("The Admin")
                .role(Role.PROVINCIAL_ADMIN).active(true)
                .build(); // no ward, no hazard

        Claims claims = service.parse(service.issue(admin));

        assertNull(claims.get("ward"));
        assertNull(claims.get("hazard"));
        assertEquals("PROVINCIAL_ADMIN", claims.get("role", String.class));
    }

    @Test
    void tokenCarriesExpiryInTheFuture() {
        Claims claims = service.parse(service.issue(recorder()));
        Instant expiry = claims.getExpiration().toInstant();
        assertTrue(expiry.isAfter(Instant.now()));
        // 480 minutes = 8 hours
        assertTrue(expiry.isBefore(Instant.now().plus(9, ChronoUnit.HOURS)));
    }

    @Test
    void tokenSignedWithDifferentKeyIsRejected() {
        JwtTokenService other = new JwtTokenService(
                "another-test-secret-key-which-is-long-enough-0123456789abcdef", 480);
        String foreignToken = other.issue(recorder());

        assertThrows(JwtException.class, () -> service.parse(foreignToken));
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = service.issue(recorder());
        String tampered = token.substring(0, token.length() - 3) + "abc";

        assertThrows(JwtException.class, () -> service.parse(tampered));
    }

    @Test
    void garbageStringIsRejected() {
        assertThrows(JwtException.class, () -> service.parse("not-a-token"));
    }

    @Test
    void shortSecretIsRejectedAtStartup() {
        assertThrows(IllegalStateException.class,
                () -> new JwtTokenService("too-short", 480));
    }
}
