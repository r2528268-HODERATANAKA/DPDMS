package com.dpdms.fire_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests of the inlined token validator against the published contract. */
class JwtServiceTest {

    private static final String SECRET = "test-secret-that-is-long-enough-for-hs256-0123456789";

    @Test
    @DisplayName("Round trip: scope claims are read back unchanged")
    void roundTrip() {
        JwtService jwt = new JwtService(SECRET, 60);

        Claims claims = jwt.parse(jwt.issue("ward4.fire", "Ward 4 Recorder",
                "WARD_RECORDER", "Ward 4", "fire"));

        assertEquals("ward4.fire", claims.getSubject());
        assertEquals("WARD_RECORDER", claims.get("role", String.class));
        assertEquals("Ward 4", claims.get("ward", String.class));
        assertEquals("fire", claims.get("hazard", String.class));
        assertEquals("Ward 4 Recorder", claims.get("name", String.class));
        assertEquals("dpdms-auth-service", claims.getIssuer());
    }

    @Test
    @DisplayName("Unscoped roles omit the ward/hazard claims")
    void unscopedRolesOmitClaims() {
        JwtService jwt = new JwtService(SECRET, 60);

        Claims claims = jwt.parse(jwt.issue("admin", "Admin", "PROVINCIAL_ADMIN", null, null));

        assertNull(claims.get("ward"));
        assertNull(claims.get("hazard"));
    }

    @Test
    @DisplayName("Foreign secret / tampered / expired tokens are rejected")
    void invalidTokensRejected() {
        JwtService jwt = new JwtService(SECRET, 60);
        JwtService impostor = new JwtService("another-secret-0123456789abcdef0123456789abcdef", 60);
        String foreign = impostor.issue("attacker", "Attacker", "PROVINCIAL_ADMIN", null, null);
        assertFalse(jwt.isValid(foreign));

        String token = jwt.issue("ward4.fire", "Ward 4 Recorder", "WARD_RECORDER", "Ward 4", "fire");
        assertFalse(jwt.isValid(token.substring(0, token.length() - 4) + "AAAA"));

        JwtService expired = new JwtService(SECRET, -1);
        assertFalse(jwt.isValid(expired.issue("x", "X", "WARD_RECORDER", "Ward 4", "fire")));
    }

    @Test
    void shortSecretRefused() {
        assertThrows(IllegalArgumentException.class, () -> new JwtService("too-short", 60));
        assertThrows(JwtException.class, () -> new JwtService(SECRET, 60).parse("not-a-token"));
    }
}
