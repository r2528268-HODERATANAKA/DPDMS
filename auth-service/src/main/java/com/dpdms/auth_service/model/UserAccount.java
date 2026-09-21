package com.dpdms.auth_service.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// One DPDMS user account.
// NOTE: we never store the raw password - only its BCrypt hash (passwordHash column).
@Entity
@Table(name = "user_accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Username is required")
    @Column(unique = true, nullable = false, length = 50)
    private String username;

    // BCrypt hash of the password - never the password itself
    @Column(nullable = false)
    private String passwordHash;

    // Full name, used as the reviewer name (reviewedBy) in the hazard services
    @NotBlank(message = "Full name is required")
    @Column(nullable = false, length = 100)
    private String fullName;

    @NotNull(message = "Role is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    // FR-SCOPE-01: ward recorders are bound to ONE concrete ward (e.g. "Mudzi").
    // Null for supervisors and admins.
    @Column(length = 50)
    private String ward;

    // FR-SCOPE-01: recorders and supervisors are bound to ONE hazard
    // ("flood", "drought", "fire", "zoonotic" or "mining"). Null for admins.
    @Column(length = 20)
    private String hazard;

    // Deactivated accounts cannot log in but their history stays intact
    @Builder.Default
    private Boolean active = true;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
