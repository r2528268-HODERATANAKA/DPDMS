package com.dpdms.auth_service.dto;

import jakarta.validation.constraints.NotBlank;

/** Login request body. */
public record LoginRequest(
        @NotBlank(message = "Username is required") String username,
        @NotBlank(message = "Password is required") String password) {
}
