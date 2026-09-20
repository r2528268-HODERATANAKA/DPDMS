package com.dpdms.auth_service.service;

import com.dpdms.auth_service.dto.LoginResponse;
import com.dpdms.auth_service.dto.UserResponse;
import com.dpdms.auth_service.dto.ValidationResponse;
import com.dpdms.auth_service.exception.InvalidCredentialsException;
import com.dpdms.auth_service.exception.ResourceNotFoundException;
import com.dpdms.auth_service.model.UserAccount;
import com.dpdms.auth_service.repository.UserAccountRepository;
import com.dpdms.auth_service.security.AuthContext;
import com.dpdms.auth_service.security.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** Login, token introspection and the current-user lookup. */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserAccountRepository repository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse login(String username, String password) {
        UserAccount account = repository.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        if (!account.isActive()) {
            throw new InvalidCredentialsException("Account is deactivated");
        }
        if (!passwordEncoder.matches(password, account.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        return new LoginResponse(
                jwtService.issue(account),
                "Bearer",
                jwtService.getExpirationMinutes(),
                account.getUsername(),
                account.getFullName(),
                account.getRole().name(),
                account.getWard(),
                account.getHazard());
    }

    /** Used by the gateway / hazard services to verify a token. */
    public ValidationResponse validate(String token) {
        Claims claims = jwtService.parse(token);
        return new ValidationResponse(
                true,
                claims.getSubject(),
                claims.get("role", String.class),
                claims.get("ward", String.class),
                claims.get("hazard", String.class),
                claims.get("name", String.class),
                claims.getExpiration().toInstant());
    }

    public UserResponse me() {
        UserAccount account = repository.findByUsername(AuthContext.username())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return UserResponse.from(account);
    }
}
