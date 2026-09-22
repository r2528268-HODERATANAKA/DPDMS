package com.dpdms.fire_service.controller;

import com.dpdms.fire_service.model.FireIncident;
import com.dpdms.fire_service.service.FireIncidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// NOTE ON HEADERS: the gateway decodes the JWT from auth-service and forwards the caller's
// identity as X-User-Name / X-User-Role / X-User-Ward / X-User-Hazard headers. Reading headers
// (instead of parsing tokens here) keeps this service simple; when run without the gateway for
// local testing you add the headers yourself with curl/Postman.
// See docs/01-ARCHITECTURE.md for the full request flow.

@RestController
@RequestMapping("/api/fires")
@RequiredArgsConstructor
public class FireIncidentController {

    private final FireIncidentService service;

    @PostMapping
    public ResponseEntity<FireIncident> create(
            @Valid @RequestBody FireIncident incident,
            @RequestHeader("X-User-Ward") String callerWard,
            @RequestHeader("X-User-Hazard") String callerHazard) {
        FireIncident created = service.create(incident, callerWard, callerHazard);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // Approved-only feed — this is what dashboard-service and report-service call
    @GetMapping
    public ResponseEntity<List<FireIncident>> getApproved() {
        return ResponseEntity.ok(service.findAllApproved());
    }

    // A recorder's own ward view, including PENDING records
    @GetMapping("/ward/{ward}")
    public ResponseEntity<List<FireIncident>> getByWard(@PathVariable String ward) {
        return ResponseEntity.ok(service.findAllForWard(ward));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FireIncident> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FireIncident> update(
            @PathVariable Long id,
            @Valid @RequestBody FireIncident incident,
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
    public ResponseEntity<FireIncident> approve(
            @PathVariable Long id,
            @RequestHeader("X-User-Name") String reviewer,
            @RequestHeader("X-User-Hazard") String callerHazard) {
        return ResponseEntity.ok(service.approve(id, reviewer, callerHazard));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<FireIncident> reject(
            @PathVariable Long id,
            @RequestHeader("X-User-Name") String reviewer,
            @RequestHeader("X-User-Hazard") String callerHazard,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.reject(id, reviewer, body.get("reason"), callerHazard));
    }

    @PatchMapping("/{id}/request-corrections")
    public ResponseEntity<FireIncident> requestCorrections(
            @PathVariable Long id,
            @RequestHeader("X-User-Name") String reviewer,
            @RequestHeader("X-User-Hazard") String callerHazard,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.requestCorrections(id, reviewer, body.get("notes"), callerHazard));
    }
}
