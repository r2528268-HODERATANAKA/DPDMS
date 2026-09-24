package com.dpdms.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Safe view of a user account - the password hash is NEVER included.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String username;
    private String fullName;
    private String role;
    private String ward;
    private String hazard;
    private Boolean active;
    private java.time.LocalDateTime createdAt;
}
