package com.dpdms.auth_service.dto;

/** Login response: the signed JWT plus the caller's profile and RBAC scope. */
public record LoginResponse(
        String token,
        String tokenType,
        long expiresInMinutes,
        String username,
        String fullName,
        String role,
        String ward,
        String hazard) {
}
