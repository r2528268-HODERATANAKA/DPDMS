package com.dpdms.auth_service.service;

import com.dpdms.auth_service.dto.CreateUserRequest;
import com.dpdms.auth_service.dto.LoginRequest;
import com.dpdms.auth_service.dto.LoginResponse;
import com.dpdms.auth_service.dto.UserResponse;
import com.dpdms.auth_service.dto.ValidationResponse;
import com.dpdms.auth_service.exception.DuplicateResourceException;
import com.dpdms.auth_service.exception.ForbiddenOperationException;
import com.dpdms.auth_service.exception.InvalidCredentialsException;
import com.dpdms.auth_service.exception.ResourceNotFoundException;
import com.dpdms.auth_service.model.Role;
import com.dpdms.auth_service.model.UserAccount;
import com.dpdms.auth_service.repository.UserAccountRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserAccountRepository repository;
    private final JwtTokenService jwtTokenService;

    // BCrypt = the standard one-way hash for passwords (salt is built into the hash string)
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ---------- Login ----------

    public LoginResponse login(LoginRequest request) {
        UserAccount user = repository.findByUsername(request.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new InvalidCredentialsException("Account has been deactivated");
        }

        return LoginResponse.builder()
                .token(jwtTokenService.issue(user))
                .tokenType("Bearer")
                .username(user.getUsername())
                .role(user.getRole().name())
                .name(user.getFullName())
                .ward(user.getWard())
                .hazard(user.getHazard())
                .build();
    }

    // ---------- Token validation (used by the gateway, and by anyone who wants to check a token) ----------

    public ValidationResponse validate(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new InvalidCredentialsException("Missing or malformed Authorization header");
        }
        String token = authorizationHeader.substring(7).trim();
        Claims claims = jwtTokenService.parse(token);

        UserAccount user = repository.findByUsername(claims.getSubject())
                .orElseThrow(() -> new InvalidCredentialsException("Unknown user in token"));
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new InvalidCredentialsException("Account has been deactivated");
        }

        return ValidationResponse.builder()
                .valid(true)
                .username(user.getUsername())
                .role(user.getRole().name())
                .name(user.getFullName())
                .ward(user.getWard())
                .hazard(user.getHazard())
                .build();
    }

    // ---------- Account management (PROVINCIAL_ADMIN only) ----------

    public UserResponse createAccount(CreateUserRequest request, String callerRole) {
        requireAdmin(callerRole);
        requireConcreteScope(request);

        if (repository.existsByUsernameIgnoreCase(request.getUsername())) {
            throw new DuplicateResourceException(
                    "Username already taken: " + request.getUsername());
        }

        UserAccount user = UserAccount.builder()
                .username(request.getUsername())
                // only the hash is stored - the raw password is thrown away
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(request.getRole())
                .ward(normalise(request.getWard()))
                .hazard(normalise(request.getHazard()))
                .active(true)
                .build();
        return toResponse(repository.save(user));
    }

    public List<UserResponse> listUsers(String callerRole) {
        requireAdmin(callerRole);
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    // ---------- Helpers ----------

    private void requireAdmin(String callerRole) {
        if (!"PROVINCIAL_ADMIN".equals(callerRole)) {
            throw new ForbiddenOperationException(
                    "Only PROVINCIAL_ADMIN accounts can manage users");
        }
    }

    // FR-SCOPE-01: recorders MUST have a concrete ward + hazard, supervisors MUST have a
    // concrete hazard. Wildcards ("*", "ALL") or blanks would silently widen a user's
    // access, so they are rejected with HTTP 400 at account creation time.
    private void requireConcreteScope(CreateUserRequest request) {
        if (request.getRole() == Role.WARD_RECORDER) {
            requireConcrete(request.getWard(), "ward");
            requireConcrete(request.getHazard(), "hazard");
        } else if (request.getRole() == Role.PROVINCIAL_SUPERVISOR) {
            requireConcrete(request.getHazard(), "hazard");
        }
        // PROVINCIAL_ADMIN needs no ward/hazard
    }

    private void requireConcrete(String value, String field) {
        if (value == null || value.isBlank()
                || value.equalsIgnoreCase("*") || value.equalsIgnoreCase("ALL")) {
            throw new IllegalArgumentException(
                    "A " + field + " account needs a concrete " + field
                            + " (wildcards like * or ALL are not allowed)");
        }
    }

    private String normalise(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private UserResponse toResponse(UserAccount user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .ward(user.getWard())
                .hazard(user.getHazard())
                .active(user.getActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
