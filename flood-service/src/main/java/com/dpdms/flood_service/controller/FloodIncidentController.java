package com.dpdms.flood_service.controller;

import com.dpdms.flood_service.model.FloodIncident;
import com.dpdms.flood_service.service.FloodIncidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// NOTE ON HEADERS: until auth-service issues real JWTs and the gateway forwards the decoded
// claims, we read the caller's ward/hazard/name from request headers so the scoping rule can
// already be exercised and tested. Swap these for @AuthenticationPrincipal claims once
// auth-service is integrated — the FloodIncidentService method signatures won't need to change.

@RestController
@RequestMapping("/api/floods")
@RequiredArgsConstructor
public class FloodIncidentController {

    private final FloodIncidentService service;

    @PostMapping
    public ResponseEntity<FloodIncident> create(
            @Valid @RequestBody FloodIncident incident,
            @RequestHeader("X-User-Ward") String callerWard,
            @RequestHeader("X-User-Hazard") String callerHazard) {
        FloodIncident created = service.create(incident, callerWard, callerHazard);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // Approved-only feed — this is what dashboard-service and report-service will call
    @GetMapping
    public ResponseEntity<List<FloodIncident>> getApproved() {
        return ResponseEntity.ok(service.findAllApproved());
    }

    // A recorder's own ward view, including PENDING records
    @GetMapping("/ward/{ward}")
    public ResponseEntity<List<FloodIncident>> getByWard(@PathVariable String ward) {
        return ResponseEntity.ok(service.findAllForWard(ward));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FloodIncident> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FloodIncident> update(
            @PathVariable Long id,
            @Valid @RequestBody FloodIncident incident,
            @RequestHeader("X-User-Ward") String callerWard,
            @RequestHeader("X-User-Hazard") String callerHazard) {
        return ResponseEntity.ok(service.update(id, incident, callerWard, callerHazard));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestHeader("X-User-Ward") String callerWard,
            @RequestHeader("X-User-Hazard") String callerHazard) {
        service.delete(id, callerWard, callerHazard);
        return ResponseEntity.noContent().build();
    }

    // ---------- Provincial supervisor actions ----------

    @PatchMapping("/{id}/approve")
    public ResponseEntity<FloodIncident> approve(
            @PathVariable Long id,
            @RequestHeader("X-User-Name") String reviewer,
            @RequestHeader("X-User-Hazard") String callerHazard) {
        return ResponseEntity.ok(service.approve(id, reviewer, callerHazard));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<FloodIncident> reject(
            @PathVariable Long id,
            @RequestHeader("X-User-Name") String reviewer,
            @RequestHeader("X-User-Hazard") String callerHazard,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.reject(id, reviewer, body.get("reason"), callerHazard));
    }

    @PatchMapping("/{id}/request-corrections")
    public ResponseEntity<FloodIncident> requestCorrections(
            @PathVariable Long id,
            @RequestHeader("X-User-Name") String reviewer,
            @RequestHeader("X-User-Hazard") String callerHazard,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.requestCorrections(id, reviewer, body.get("notes"), callerHazard));
    }
}
