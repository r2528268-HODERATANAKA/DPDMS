package com.dpdms.auth_service.dto;

import com.dpdms.auth_service.model.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Request body of POST /api/auth/users (PROVINCIAL_ADMIN creates accounts).
// ward/hazard are optional at the JSON level - AuthService enforces which
// role needs which of them (FR-SCOPE-01).
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

    @NotBlank(message = "Username is required")
    private String username;

    // Raw password (min 8 chars). It is hashed with BCrypt before saving - never stored as-is.
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotNull(message = "Role is required")
    private Role role;

    // Required (concrete) for WARD_RECORDER
    private String ward;

    // Required (concrete) for WARD_RECORDER and PROVINCIAL_SUPERVISOR
    private String hazard;
}
