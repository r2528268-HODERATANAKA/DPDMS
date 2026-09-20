package com.dpdms.auth_service.controller;

import com.dpdms.auth_service.dto.LoginRequest;
import com.dpdms.auth_service.dto.LoginResponse;
import com.dpdms.auth_service.dto.UserResponse;
import com.dpdms.auth_service.dto.ValidationResponse;
import com.dpdms.auth_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints:
 *   POST /api/v1/auth/login     public - issue a signed JWT
 *   GET  /api/v1/auth/me        authenticated - current profile
 *   GET  /api/v1/auth/validate  verify a token (used by the gateway / hazard services)
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.username().trim(), request.password());
    }

    @GetMapping("/me")
    public UserResponse me() {
        return authService.me();
    }

    @GetMapping("/validate")
    public ValidationResponse validate(@RequestHeader("Authorization") String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header must be 'Bearer <token>'");
        }
        return authService.validate(authorization.substring(7));
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("auth-service OK");
    }
}
