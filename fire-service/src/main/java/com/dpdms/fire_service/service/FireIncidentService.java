package com.dpdms.fire_service.service;

import com.dpdms.fire_service.event.IncidentEvent;
import com.dpdms.fire_service.event.IncidentEventPublisher;
import com.dpdms.fire_service.exception.ResourceNotFoundException;
import com.dpdms.fire_service.model.FireIncident;
import com.dpdms.fire_service.model.IncidentStatus;
import com.dpdms.fire_service.repository.FireIncidentRepository;
import com.dpdms.fire_service.security.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Fire incident business logic: RBAC scoping, the workflow state machine and the
 * asynchronous alert publication on approval.
 *
 * Every method first consults the JWT scope via AuthContext - the backend half of the
 * RBAC matrix (the frontend only hides UI).
 */
@Service
@RequiredArgsConstructor
public class FireIncidentService {

    public static final String HAZARD = "fire";

    private final FireIncidentRepository repository;
    private final IncidentEventPublisher eventPublisher;

    // ------------------------------------------------------------------
    // Queries (visibility per role)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<FireIncident> list() {
        AuthContext.require(AuthContext.canAccessHazard(HAZARD),
                "Your scope does not include the fire hazard");
        if (AuthContext.isWardRecorder()) {
            return repository.findByWard(AuthContext.ward());
        }
        return repository.findAll();
    }

    /** Approved-only feed for the dashboard/report services. */
    @Transactional(readOnly = true)
    public List<FireIncident> listApproved() {
        AuthContext.require(AuthContext.canAccessHazard(HAZARD),
                "Your scope does not include the fire hazard");
        return repository.findByStatus(IncidentStatus.APPROVED);
    }

    @Transactional(readOnly = true)
    public List<FireIncident> listForWard(String ward) {
        AuthContext.require(AuthContext.canAccessHazard(HAZARD),
                "Your scope does not include the fire hazard");
        AuthContext.require(AuthContext.canAccessWard(ward),
                "You may only view incidents of your own ward");
        return repository.findByWard(ward);
    }

    @Transactional(readOnly = true)
    public FireIncident get(Long id) {
        FireIncident incident = find(id);
        AuthContext.require(AuthContext.canAccessHazard(HAZARD)
                        && AuthContext.canAccessWard(incident.getWard()),
                "You may not view this incident");
        return incident;
    }

    // ------------------------------------------------------------------
    // Commands (capture + workflow)
    // ------------------------------------------------------------------

    @Transactional
    public FireIncident create(FireIncident incident) {
        AuthContext.require(AuthContext.canCaptureIncidents(), "Only ward recorders capture incidents");
        AuthContext.require(AuthContext.canAccessHazard(HAZARD),
                "Your scope does not include the fire hazard");
        AuthContext.require(AuthContext.canAccessWard(incident.getWard()),
                "You may only capture incidents for your own ward (" + AuthContext.ward() + ")");

        incident.setId(null);
        incident.setStatus(IncidentStatus.PENDING);
        incident.setReviewedBy(null);
        incident.setReviewedAt(null);
        incident.setReviewNotes(null);
        incident.setCreatedAt(LocalDateTime.now());
        return repository.save(incident);
    }

    @Transactional
    public FireIncident update(Long id, FireIncident updated) {
        AuthContext.require(AuthContext.canCaptureIncidents(), "Only ward recorders edit incidents");
        AuthContext.require(AuthContext.canAccessHazard(HAZARD),
                "Your scope does not include the fire hazard");

        FireIncident existing = find(id);
        AuthContext.require(AuthContext.canAccessWard(existing.getWard()),
                "You may only edit incidents of your own ward");
        AuthContext.require(AuthContext.canAccessWard(updated.getWard()),
                "You may only move an incident within your own ward");
        if (!existing.isEditable()) {
            throw new IllegalStateException(
                    "Incident is " + existing.getStatus() + " and can no longer be edited");
        }

        copyFields(existing, updated);
        existing.setStatus(IncidentStatus.PENDING); // resubmitted after edit
        return repository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        AuthContext.require(AuthContext.canCaptureIncidents(), "Only ward recorders delete incidents");
        AuthContext.require(AuthContext.canAccessHazard(HAZARD),
                "Your scope does not include the fire hazard");

        FireIncident existing = find(id);
        AuthContext.require(AuthContext.canAccessWard(existing.getWard()),
                "You may only delete incidents of your own ward");
        repository.delete(existing);
    }

    public FireIncident approve(Long id, String comment) {
        return review(id, IncidentStatus.APPROVED, comment);
    }

    public FireIncident reject(Long id, String comment) {
        return review(id, IncidentStatus.REJECTED, comment);
    }

    public FireIncident requestCorrections(Long id, String comment) {
        return review(id, IncidentStatus.CORRECTIONS_REQUESTED, comment);
    }

    @Transactional
    public FireIncident review(Long id, IncidentStatus decision, String comment) {
        AuthContext.require(AuthContext.canReview(HAZARD),
                "You are not allowed to review fire incidents");

        FireIncident incident = find(id);
        incident.applyReview(decision, AuthContext.fullName(), comment);
        FireIncident saved = repository.save(incident);

        if (decision == IncidentStatus.APPROVED) {
            publishApproved(saved);
        }
        return saved;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void publishApproved(FireIncident incident) {
        IncidentEvent event = new IncidentEvent();
        event.setIncidentId(incident.getId());
        event.setHazardType(incident.getHazardType());
        event.setWard(incident.getWard());
        event.setDistrict(incident.getDistrict());
        event.setProvince(incident.getProvince());
        event.setSeverity(incident.getSeverity().name());
        event.setSeverityRank(incident.getSeverity().rank()); // polymorphic call
        event.setLatitude(incident.getLatitude());
        event.setLongitude(incident.getLongitude());
        event.setOccurredAt(incident.getOccurredAt() == null ? null : incident.getOccurredAt().toString());
        event.setReporter(incident.getReporter());
        event.setRecommendedActions(List.of(
                "Alert the nearest fire response team",
                "Establish a firebreak ahead of the fire front in " + incident.getWard(),
                "Move livestock and people out of the fire path"));
        IncidentEventPublisher.approvedNow(event, AuthContext.fullName());
        try {
            eventPublisher.publishApproved(event);
        } catch (Exception e) {
            // FR-ALR: alert fan-out is asynchronous - approval must never fail or block
            // because the broker is unavailable. The audit trail stays intact.
        }
    }

    private FireIncident find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fire incident not found: " + id));
    }

    private void copyFields(FireIncident target, FireIncident src) {
        target.setWard(src.getWard());
        target.setDistrict(src.getDistrict());
        target.setProvince(src.getProvince());
        target.setOccurredAt(src.getOccurredAt());
        target.setReporter(src.getReporter());
        target.setSeverity(src.getSeverity());
        target.setLatitude(src.getLatitude());
        target.setLongitude(src.getLongitude());
        target.setAreaBurnedHectares(src.getAreaBurnedHectares());
        target.setSuspectedCause(src.getSuspectedCause());
        target.setInjuries(src.getInjuries());
        target.setFatalities(src.getFatalities());
        target.setStructuresDestroyed(src.getStructuresDestroyed());
        target.setFireStatus(src.getFireStatus());
    }
}
