package com.dpdms.auth_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A DPDMS user account.
 *
 * The RBAC scope of the SDD is carried by:
 *   role   - one of the three brief roles
 *   ward   - the ONE ward of a ward recorder (null for supervisors/admins)
 *   hazard - the ONE hazard of a recorder/supervisor (null for provincial admins)
 */
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
    @Column(unique = true, nullable = false, length = 60)
    private String username;

    // BCrypt hash - the raw password is never stored and never returned by any endpoint
    @NotBlank(message = "Password is required")
    @Column(nullable = false)
    private String passwordHash;

    @NotBlank(message = "Full name is required")
    @Column(nullable = false, length = 120)
    private String fullName;

    @Column(length = 120)
    private String email;

    /** Phone in international format, used by the WhatsApp alert channel. */
    @Column(length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Role is required")
    @Column(nullable = false, length = 30)
    private Role role;

    /** Concrete ward for WARD_RECORDER (e.g. "Ward 4"); null for supervisors/admins. */
    @Column(length = 60)
    private String ward;

    /** Hazard this account is scoped to (e.g. "fire"); null for provincial admins. */
    @Column(length = 30)
    private String hazard;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
