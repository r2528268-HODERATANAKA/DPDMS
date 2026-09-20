package com.dpdms.auth_service.controller;

import com.dpdms.auth_service.dto.CreateUserRequest;
import com.dpdms.auth_service.dto.UserResponse;
import com.dpdms.auth_service.service.UserAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** User administration - PROVINCIAL_ADMIN only (enforced in UserAccountService). */
@RestController
@RequestMapping("/api/v1/auth/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserAccountService service;

    @GetMapping
    public List<UserResponse> list() {
        return service.findAll();
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}/status")
    public UserResponse toggleStatus(@PathVariable Long id) {
        return service.toggleStatus(id);
    }
}
