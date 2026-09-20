package com.dpdms.auth_service.dto;

import java.time.Instant;

/** Response of GET /api/v1/auth/validate - the decoded scope of a token. */
public record ValidationResponse(
        boolean valid,
        String username,
        String role,
        String ward,
        String hazard,
        String fullName,
        Instant expiresAt) {
}
