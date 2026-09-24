package com.dpdms.zoonotic_disease_service.controller;

import com.dpdms.zoonotic_disease_service.model.ZoonoticIncident;
import com.dpdms.zoonotic_disease_service.service.ZoonoticIncidentService;
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
@RequestMapping("/api/zoonotics")
@RequiredArgsConstructor
public class ZoonoticIncidentController {

    private final ZoonoticIncidentService service;

    @PostMapping
    public ResponseEntity<ZoonoticIncident> create(
            @Valid @RequestBody ZoonoticIncident incident,
            @RequestHeader("X-User-Ward") String callerWard,
            @RequestHeader("X-User-Hazard") String callerHazard) {
        ZoonoticIncident created = service.create(incident, callerWard, callerHazard);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // Approved-only feed — this is what dashboard-service and report-service call
    @GetMapping
    public ResponseEntity<List<ZoonoticIncident>> getApproved() {
        return ResponseEntity.ok(service.findAllApproved());
    }

    // Role-scoped list for the web dashboard (recorders: own ward; supervisors/admins: all statuses)
    @GetMapping("/scoped")
    public ResponseEntity<List<ZoonoticIncident>> getScoped(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Ward", required = false) String ward,
            @RequestHeader(value = "X-User-Hazard", required = false) String hazard) {
        return ResponseEntity.ok(service.findAllScoped(role, ward, hazard));
    }

    // A recorder's own ward view, including PENDING records
    @GetMapping("/ward/{ward}")
    public ResponseEntity<List<ZoonoticIncident>> getByWard(@PathVariable String ward) {
        return ResponseEntity.ok(service.findAllForWard(ward));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ZoonoticIncident> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ZoonoticIncident> update(
            @PathVariable Long id,
            @Valid @RequestBody ZoonoticIncident incident,
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
    public ResponseEntity<ZoonoticIncident> approve(
            @PathVariable Long id,
            @RequestHeader("X-User-Name") String reviewer,
            @RequestHeader("X-User-Hazard") String callerHazard) {
        return ResponseEntity.ok(service.approve(id, reviewer, callerHazard));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<ZoonoticIncident> reject(
            @PathVariable Long id,
            @RequestHeader("X-User-Name") String reviewer,
            @RequestHeader("X-User-Hazard") String callerHazard,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.reject(id, reviewer, body.get("reason"), callerHazard));
    }

    @PatchMapping("/{id}/request-corrections")
    public ResponseEntity<ZoonoticIncident> requestCorrections(
            @PathVariable Long id,
            @RequestHeader("X-User-Name") String reviewer,
            @RequestHeader("X-User-Hazard") String callerHazard,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.requestCorrections(id, reviewer, body.get("notes"), callerHazard));
    }
}
