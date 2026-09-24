package com.dpdms.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Response of GET /api/auth/validate - tells the caller (usually the gateway)
// who is behind a token and whether the account is still usable.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResponse {

    private boolean valid;
    private String username;
    private String role;
    private String name;
    private String ward;
    private String hazard;
}
