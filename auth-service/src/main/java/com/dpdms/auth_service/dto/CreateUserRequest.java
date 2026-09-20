package com.dpdms.auth_service.dto;

import com.dpdms.auth_service.model.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Account creation request - only PROVINCIAL_ADMIN tokens may submit this.
 * The (ward, hazard) scoping rules are validated in UserAccountService (FR-SCOPE-01).
 */
public record CreateUserRequest(
        @NotBlank(message = "Username is required") String username,
        @NotBlank(message = "Password is required") String password,
        @NotBlank(message = "Full name is required") String fullName,
        String email,
        String phone,
        @NotNull(message = "Role is required") Role role,
        String ward,
        String hazard) {
}
