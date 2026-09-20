package com.dpdms.auth_service.service;

import com.dpdms.auth_service.dto.CreateUserRequest;
import com.dpdms.auth_service.dto.UserResponse;
import com.dpdms.auth_service.exception.DuplicateResourceException;
import com.dpdms.auth_service.exception.ForbiddenOperationException;
import com.dpdms.auth_service.exception.ResourceNotFoundException;
import com.dpdms.auth_service.model.Role;
import com.dpdms.auth_service.model.UserAccount;
import com.dpdms.auth_service.repository.UserAccountRepository;
import com.dpdms.auth_service.security.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/** User administration - PROVINCIAL_ADMIN only (RBAC matrix). */
@Service
@RequiredArgsConstructor
public class UserAccountService {

    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse create(CreateUserRequest request) {
        requireAdmin();

        // FR-SCOPE-01: a ward recorder must be pinned to ONE concrete ward AND ONE hazard.
        // Wildcards ("*", "ALL", blank) would let a token act province-wide, so they are rejected.
        if (request.role() == Role.WARD_RECORDER) {
            requireConcreteScope(request.ward(), "ward");
            requireConcreteScope(request.hazard(), "hazard");
        }
        // A provincial supervisor reviews all wards for ONE hazard.
        if (request.role() == Role.PROVINCIAL_SUPERVISOR) {
            requireConcreteScope(request.hazard(), "hazard");
        }
        if (repository.existsByUsernameIgnoreCase(request.username())) {
            throw new DuplicateResourceException("Username already exists: " + request.username());
        }
        if (request.password() == null || request.password().length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }

        UserAccount account = UserAccount.builder()
                .username(request.username().trim())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .email(request.email())
                .phone(request.phone())
                .role(request.role())
                .ward(normalise(request.ward()))
                .hazard(normalise(request.hazard()))
                .active(true)
                .build();

        return UserResponse.from(repository.save(account));
    }

    public List<UserResponse> findAll() {
        requireAdmin();
        return repository.findAll().stream().map(UserResponse::from).toList();
    }

    public UserResponse toggleStatus(Long id) {
        requireAdmin();
        UserAccount account = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        account.setActive(!account.isActive());
        return UserResponse.from(repository.save(account));
    }

    private void requireAdmin() {
        if (!AuthContext.isProvincialAdmin()) {
            throw new ForbiddenOperationException("Only a PROVINCIAL_ADMIN can manage user accounts");
        }
    }

    private void requireConcreteScope(String value, String field) {
        if (value == null || value.isBlank() || "*".equals(value.trim()) || "ALL".equalsIgnoreCase(value.trim())) {
            throw new IllegalArgumentException(
                    "A ward recorder needs a concrete " + field + " - wildcards are not allowed (FR-SCOPE-01)");
        }
    }

    private String normalise(String value) {
        return (value == null || value.isBlank() || "*".equalsIgnoreCase(value.trim())) ? null : value.trim();
    }
}
