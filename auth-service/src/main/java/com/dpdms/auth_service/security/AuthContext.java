package com.dpdms.auth_service.security;

import com.dpdms.auth_service.exception.ForbiddenOperationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

/**
 * Static RBAC helper: reads the scope placed by {@link JwtAuthFilter} and answers the
 * permission-matrix questions of the SDD. Used by every service's controllers/services.
 *
 * Missing ward/hazard claims (supervisors, admins) are treated as the "*" wildcard.
 */
public final class AuthContext {

    private AuthContext() { }

    public static Authentication auth() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public static String username() {
        Authentication a = auth();
        return a != null ? a.getName() : null;
    }

    public static String role() {
        Authentication a = auth();
        if (a == null) {
            return null;
        }
        return a.getAuthorities().stream()
                .findFirst()
                .map(Object::toString)
                .map(s -> s.replaceFirst("^ROLE_", ""))
                .orElse(null);
    }

    private static String detail(String key) {
        Authentication a = auth();
        if (a != null && a.getDetails() instanceof Map<?, ?> details && details.get(key) != null) {
            return String.valueOf(details.get(key));
        }
        return "*";
    }

    /** The single ward of a ward recorder, or "*" for unscoped roles. */
    public static String ward() { return detail("ward"); }

    /** The single hazard of recorder/supervisor, or "*" for unscoped roles. */
    public static String hazard() { return detail("hazard"); }

    /** The caller's full name (used as the reviewer stamp), or null. */
    public static String fullName() {
        Authentication a = auth();
        return a != null && a.getDetails() instanceof Map<?, ?> details && details.get("name") != null
                ? String.valueOf(details.get("name")) : null;
    }

    // ------------------------------------------------------------------
    // Role predicates (the three brief roles)
    // ------------------------------------------------------------------

    public static boolean isWardRecorder()         { return "WARD_RECORDER".equals(role()); }
    public static boolean isProvincialSupervisor() { return "PROVINCIAL_SUPERVISOR".equals(role()); }
    public static boolean isProvincialAdmin()      { return "PROVINCIAL_ADMIN".equals(role()); }

    /** No token, or Spring Security's anonymous token -> not authenticated. */
    private static boolean isAnonymous() {
        String r = role();
        return r == null || "ANONYMOUS".equals(r);
    }

    // ------------------------------------------------------------------
    // Permission-matrix rules (single source of truth for enforcement)
    // ------------------------------------------------------------------

    /** May the caller see and work with incidents of this hazard? */
    public static boolean canAccessHazard(String hazardSlug) {
        if (isAnonymous()) {
            return false;
        }
        if (isProvincialAdmin()) {
            return true;
        }
        return "*".equals(hazard()) || hazardSlug.equalsIgnoreCase(hazard());
    }

    /** Ward recorder sees only their own ward; other roles see all wards. */
    public static boolean canAccessWard(String wardName) {
        if (isAnonymous()) {
            return false;
        }
        return !isWardRecorder() || wardName.equalsIgnoreCase(ward());
    }

    /** Approve / reject / request corrections on incidents of this hazard. */
    public static boolean canReview(String hazardSlug) {
        return isProvincialAdmin() || (isProvincialSupervisor() && canAccessHazard(hazardSlug));
    }

    /** Capture / edit incidents (ward recorders capture; provincial admins may fix). */
    public static boolean canCaptureIncidents() {
        return isWardRecorder() || isProvincialAdmin();
    }

    /** May the caller request reports / downloads? */
    public static boolean canGenerateReports() {
        return !isWardRecorder();
    }

    public static void require(boolean condition, String message) {
        if (!condition) {
            throw new ForbiddenOperationException(message);
        }
    }
}
