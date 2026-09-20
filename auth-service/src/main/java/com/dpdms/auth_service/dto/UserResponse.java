package com.dpdms.auth_service.dto;

import com.dpdms.auth_service.model.UserAccount;

/** Safe user projection - never exposes the password hash. */
public record UserResponse(
        Long id,
        String username,
        String fullName,
        String email,
        String phone,
        String role,
        String ward,
        String hazard,
        boolean active) {

    public static UserResponse from(UserAccount u) {
        return new UserResponse(
                u.getId(), u.getUsername(), u.getFullName(), u.getEmail(), u.getPhone(),
                u.getRole().name(), u.getWard(), u.getHazard(), u.isActive());
    }
}
