package com.dpdms.auth_service.controller;

import com.dpdms.auth_service.dto.CreateUserRequest;
import com.dpdms.auth_service.dto.LoginRequest;
import com.dpdms.auth_service.dto.LoginResponse;
import com.dpdms.auth_service.dto.UserResponse;
import com.dpdms.auth_service.dto.ValidationResponse;
import com.dpdms.auth_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Auth endpoints:
//   POST /api/auth/login            - everyone        -> returns a JWT
//   GET  /api/auth/validate         - everyone        -> checks a Bearer token
//   POST /api/auth/users            - PROVINCIAL_ADMIN (X-User-Role header, set by the
//                                      gateway after it verified the Bearer token)
//   GET  /api/auth/users            - PROVINCIAL_ADMIN
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(service.login(request));
    }

    // The gateway calls this once per request and forwards the claims as X-User-* headers
    @GetMapping("/validate")
    public ResponseEntity<ValidationResponse> validate(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResponseEntity.ok(service.validate(authorization));
    }

    @PostMapping("/users")
    public ResponseEntity<UserResponse> createAccount(
            @Valid @RequestBody CreateUserRequest request,
            // Header set by the gateway from the verified JWT (see docs/01-ARCHITECTURE.md)
            @RequestHeader("X-User-Role") String callerRole) {
        UserResponse created = service.createAccount(request, callerRole);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> listUsers(
            @RequestHeader("X-User-Role") String callerRole) {
        return ResponseEntity.ok(service.listUsers(callerRole));
    }
}
