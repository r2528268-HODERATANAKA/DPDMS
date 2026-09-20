package com.dpdms.auth_service.controller;

import com.dpdms.auth_service.dto.RecipientDto;
import com.dpdms.auth_service.model.Role;
import com.dpdms.auth_service.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * INTERNAL service-to-service endpoint used by alert-service to resolve who must be
 * notified for an approved incident. Never routed through the gateway.
 */
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalApiController {

    private final UserAccountRepository users;

    @GetMapping("/recipients")
    public List<RecipientDto> recipients(@RequestParam String ward, @RequestParam String hazard) {
        List<RecipientDto> out = new ArrayList<>();

        users.findByRoleAndWardAndHazard(Role.WARD_RECORDER, ward, hazard)
                .forEach(u -> out.add(new RecipientDto(u.getFullName(), u.getEmail(), u.getPhone())));

        users.findByRoleAndHazard(Role.PROVINCIAL_SUPERVISOR, hazard)
                .forEach(u -> out.add(new RecipientDto(u.getFullName(), u.getEmail(), u.getPhone())));

        users.findByRole(Role.PROVINCIAL_ADMIN)
                .forEach(u -> out.add(new RecipientDto(u.getFullName(), u.getEmail(), u.getPhone())));

        return out;
    }
}
