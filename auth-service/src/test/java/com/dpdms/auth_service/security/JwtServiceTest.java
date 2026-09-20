package com.dpdms.auth_service.security;

import com.dpdms.auth_service.model.Role;
import com.dpdms.auth_service.model.UserAccount;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The token contract tests: whatever claims auth-service puts into a token is what the
 * five hazard services read - these must never drift.
 */
class JwtServiceTest {

    private static final String SECRET =
            "test-secret-that-is-long-enough-for-hs256-0123456789";

    private UserAccount wardRecorder() {
        return UserAccount.builder()
                .username("ward4.fire").fullName("Ward 4 Fire Recorder")
                .role(Role.WARD_RECORDER).ward("Ward 4").hazard("fire").active(true).build();
    }

    @Test
    @DisplayName("Issued token carries the full (ward, hazard, name) contract")
    void issuedTokenCarriesScopeClaims() {
        JwtService jwt = new JwtService(SECRET, 60);

        Claims claims = jwt.parse(jwt.issue(wardRecorder()));

        assertEquals("ward4.fire", claims.getSubject());
        assertEquals("WARD_RECORDER", claims.get("role", String.class));
        assertEquals("Ward 4", claims.get("ward", String.class));
        assertEquals("fire", claims.get("hazard", String.class));
        assertEquals("Ward 4 Fire Recorder", claims.get("name", String.class));
        assertEquals("dpdms-auth-service", claims.getIssuer());
        assertTrue(jwt.isValid(jwt.issue(wardRecorder())));
    }

    @Test
    @DisplayName("Supervisor token has a hazard but no ward claim")
    void supervisorTokenHasHazardOnly() {
        JwtService jwt = new JwtService(SECRET, 60);
        UserAccount supervisor = UserAccount.builder()
                .username("fire.supervisor").fullName("Fire Supervisor")
                .role(Role.PROVINCIAL_SUPERVISOR).hazard("fire").active(true).build();

        Claims claims = jwt.parse(jwt.issue(supervisor));

        assertEquals("fire", claims.get("hazard", String.class));
        assertNull(claims.get("ward"));
    }

    @Test
    @DisplayName("A token signed with a different team secret is rejected")
    void foreignSecretRejected() {
        JwtService mine = new JwtService(SECRET, 60);
        JwtService impostor = new JwtService("another-team-secret-0123456789abcdef0123456789abcdef", 60);

        String forged = impostor.issue(wardRecorder());

        assertFalse(mine.isValid(forged), "services must reject tokens from a foreign secret");
        assertThrows(JwtException.class, () -> mine.parse(forged));
    }

    @Test
    @DisplayName("A tampered payload invalidates the signature")
    void tamperedTokenRejected() {
        JwtService jwt = new JwtService(SECRET, 60);
        String token = jwt.issue(wardRecorder());

        assertFalse(jwt.isValid(token.substring(0, token.length() - 4) + "AAAA"));
    }

    @Test
    @DisplayName("Expired tokens are rejected")
    void expiredTokenRejected() {
        JwtService expired = new JwtService(SECRET, -1);
        JwtService validator = new JwtService(SECRET, 60);

        assertFalse(validator.isValid(expired.issue(wardRecorder())));
    }

    @Test
    @DisplayName("Secrets shorter than 32 characters are refused at startup")
    void shortSecretRefused() {
        assertThrows(IllegalArgumentException.class, () -> new JwtService("too-short", 60));
    }
}
