package com.dpdms.fire_service.controller;

import com.dpdms.fire_service.dto.ReviewDecision;
import com.dpdms.fire_service.model.FireIncident;
import com.dpdms.fire_service.service.FireIncidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints of the fire service (gateway routes /api/v1/fire/**):
 *
 *   POST   /api/v1/fire/incidents                       capture (ward recorder, own ward)
 *   GET    /api/v1/fire/incidents                       list (role-scoped)
 *   GET    /api/v1/fire/incidents/approved              approved-only feed
 *   GET    /api/v1/fire/incidents/ward/{ward}           a ward's records
 *   GET    /api/v1/fire/incidents/{id}                  single
 *   PUT    /api/v1/fire/incidents/{id}                  edit + resubmit
 *   DELETE /api/v1/fire/incidents/{id}                  delete (own ward)
 *   POST   /api/v1/fire/incidents/{id}/approve
 *   POST   /api/v1/fire/incidents/{id}/reject
 *   POST   /api/v1/fire/incidents/{id}/request-corrections
 */
@RestController
@RequestMapping("/api/v1/fire/incidents")
@RequiredArgsConstructor
public class FireIncidentController {

    private final FireIncidentService service;

    @GetMapping
    public List<FireIncident> list() {
        return service.list();
    }

    @GetMapping("/approved")
    public List<FireIncident> approved() {
        return service.listApproved();
    }

    @GetMapping("/ward/{ward}")
    public List<FireIncident> byWard(@PathVariable String ward) {
        return service.listForWard(ward);
    }

    @GetMapping("/{id}")
    public FireIncident get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<FireIncident> create(@Valid @RequestBody FireIncident incident) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(incident));
    }

    @PutMapping("/{id}")
    public FireIncident update(@PathVariable Long id, @Valid @RequestBody FireIncident incident) {
        return service.update(id, incident);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/approve")
    public FireIncident approve(@PathVariable Long id,
                                @RequestBody(required = false) ReviewDecision decision) {
        return service.approve(id, decision == null ? null : decision.comment());
    }

    @PostMapping("/{id}/reject")
    public FireIncident reject(@PathVariable Long id,
                               @RequestBody(required = false) ReviewDecision decision) {
        return service.reject(id, decision == null ? null : decision.comment());
    }

    @PostMapping("/{id}/request-corrections")
    public FireIncident requestCorrections(@PathVariable Long id,
                                           @RequestBody(required = false) ReviewDecision decision) {
        return service.requestCorrections(id, decision == null ? null : decision.comment());
    }
}
