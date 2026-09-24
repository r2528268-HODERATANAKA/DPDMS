package com.dpdms.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Response of POST /api/auth/login.
// The frontend stores "token" and sends it back as: Authorization: Bearer <token>
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String tokenType;   // always "Bearer"
    private String username;
    private String role;        // WARD_RECORDER / PROVINCIAL_SUPERVISOR / PROVINCIAL_ADMIN
    private String name;        // full name - becomes the reviewer name on approvals
    private String ward;        // recorders only
    private String hazard;      // recorders + supervisors
}
