package org.example.zoonoticdiseaseservice.controller;

import jakarta.validation.Valid;
import org.example.zoonoticdiseaseservice.entity.ApprovalAudit;
import org.example.zoonoticdiseaseservice.entity.ZoonoticDisease;
import org.example.zoonoticdiseaseservice.security.AccessControlService;
import org.example.zoonoticdiseaseservice.service.ZoonoticDiseaseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/zoonotic-diseases")
public class ZoonoticDiseaseController {

    private final ZoonoticDiseaseService service;
    private final AccessControlService accessControlService;

    public ZoonoticDiseaseController(
            ZoonoticDiseaseService service,
            AccessControlService accessControlService) {

        this.service = service;
        this.accessControlService = accessControlService;
    }

    // GET ALL
    @GetMapping
    public List<ZoonoticDisease> getAllDiseases() {
        return service.getAllDiseases();
    }

    // GET BY ID
    @GetMapping("/{id}")
    public ResponseEntity<ZoonoticDisease> getDiseaseById(
            @PathVariable Long id) {

        return service.getDiseaseById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // CREATE
    @PostMapping
    public ZoonoticDisease createDisease(
            @Valid @RequestBody ZoonoticDisease disease) {

        return service.createDisease(disease);
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<ZoonoticDisease> updateDisease(
            @PathVariable Long id,
            @Valid @RequestBody ZoonoticDisease disease) {

        try {
            return ResponseEntity.ok(
                    service.updateDisease(id, disease));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDisease(
            @PathVariable Long id) {

        try {
            service.deleteDisease(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // APPROVE
    @PutMapping("/{id}/approve")
    public ResponseEntity<ZoonoticDisease> approveDisease(
            @PathVariable Long id) {

        try {
            return ResponseEntity.ok(
                    service.approveDisease(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // REJECT
    @PutMapping("/{id}/reject")
    public ResponseEntity<ZoonoticDisease> rejectDisease(
            @PathVariable Long id,
            @RequestParam String reason) {

        try {
            return ResponseEntity.ok(
                    service.rejectDisease(id, reason));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // REQUEST CORRECTIONS
    @PutMapping("/{id}/request-corrections")
    public ResponseEntity<ZoonoticDisease> requestCorrections(
            @PathVariable Long id,
            @RequestParam String reason) {

        try {
            return ResponseEntity.ok(
                    service.requestCorrections(id, reason));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // GET AUDIT HISTORY
    @GetMapping("/{id}/audit")
    public ResponseEntity<List<ApprovalAudit>> getAuditHistory(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                service.getAuditHistory(id));
    }
}