package com.dpdms.auth_service.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests of the RBAC permission matrix for the three brief roles. */
class AuthContextTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(String role, String ward, String hazard) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "test.user", "n/a", List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        Map<String, Object> details = new HashMap<>();
        details.put("ward", ward);
        details.put("hazard", hazard);
        details.put("name", "Test User");
        auth.setDetails(details);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Ward recorder is scoped to one (ward, hazard) pair")
    void wardRecorderScope() {
        loginAs("WARD_RECORDER", "Ward 4", "fire");

        assertEquals("WARD_RECORDER", AuthContext.role());
        assertEquals("Ward 4", AuthContext.ward());
        assertTrue(AuthContext.canAccessHazard("fire"));
        assertFalse(AuthContext.canAccessHazard("flood"));
        assertTrue(AuthContext.canAccessWard("Ward 4"));
        assertFalse(AuthContext.canAccessWard("Ward 9"));
        assertFalse(AuthContext.canReview("fire"));
        assertTrue(AuthContext.canCaptureIncidents());
        assertFalse(AuthContext.canGenerateReports());
    }

    @Test
    @DisplayName("Provincial supervisor reviews own hazard across all wards")
    void provincialSupervisorScope() {
        loginAs("PROVINCIAL_SUPERVISOR", null, "fire");

        assertEquals("*", AuthContext.ward(), "missing ward claim is the wildcard");
        assertTrue(AuthContext.canAccessHazard("fire"));
        assertFalse(AuthContext.canAccessHazard("mining"));
        assertTrue(AuthContext.canAccessWard("Ward 9"));
        assertTrue(AuthContext.canReview("fire"));
        assertFalse(AuthContext.canReview("flood"));
        assertFalse(AuthContext.canCaptureIncidents());
        assertTrue(AuthContext.canGenerateReports());
    }

    @Test
    @DisplayName("Provincial admin can do everything except capture")
    void provincialAdminScope() {
        loginAs("PROVINCIAL_ADMIN", null, null);

        assertTrue(AuthContext.canAccessHazard("flood"));
        assertTrue(AuthContext.canAccessHazard("fire"));
        assertTrue(AuthContext.canReview("fire"));
        assertTrue(AuthContext.canGenerateReports());
    }

    @Test
    @DisplayName("No authentication -> no role, no access")
    void anonymousHasNoAccess() {
        assertFalse(AuthContext.canAccessHazard("fire"));
        assertFalse(AuthContext.canReview("fire"));
        assertFalse(AuthContext.canCaptureIncidents());
    }
}
