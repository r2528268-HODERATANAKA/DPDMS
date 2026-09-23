package com.dpdms.zoonotic_disease_service.service;

import com.dpdms.zoonotic_disease_service.exception.ForbiddenOperationException;
import com.dpdms.zoonotic_disease_service.exception.ResourceNotFoundException;
import com.dpdms.zoonotic_disease_service.model.ZoonoticIncident;
import com.dpdms.zoonotic_disease_service.model.IncidentStatus;
import com.dpdms.zoonotic_disease_service.repository.ZoonoticIncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ZoonoticIncidentService {

    private final ZoonoticIncidentRepository repository;
    private final AlertNotifier alertNotifier;

    // ---------- Create ----------
    // NOTE: once auth-service issues JWTs and the gateway decodes them, callerWard/callerHazard
    // will arrive from the token's claims instead of headers — the scoping rules stay the same.
    public ZoonoticIncident create(ZoonoticIncident incident, String callerWard, String callerHazard) {
        enforceHazardScope(callerHazard);
        enforceWardScope(incident.getWard(), callerWard);
        incident.setStatus(IncidentStatus.PENDING);
        incident.setCreatedAt(LocalDateTime.now());
        return repository.save(incident);
    }

    // ---------- Read ----------

    public List<ZoonoticIncident> findAllApproved() {
        return repository.findByStatus(IncidentStatus.APPROVED);
    }

    public List<ZoonoticIncident> findAllForWard(String ward) {
        return repository.findByWard(ward);
    }

    public ZoonoticIncident findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zoonotic disease incident " + id + " not found"));
    }

    // ---------- Update (recorder editing a record still in PENDING or sent back for corrections) ----------

    public ZoonoticIncident update(Long id, ZoonoticIncident updated, String callerWard, String callerHazard) {
        enforceHazardScope(callerHazard);
        ZoonoticIncident existing = findById(id);
        enforceWardScope(existing.getWard(), callerWard);

        if (existing.getStatus() == IncidentStatus.APPROVED) {
            throw new ForbiddenOperationException("Approved records cannot be edited directly");
        }

        existing.setDistrict(updated.getDistrict());
        existing.setProvince(updated.getProvince());
        existing.setOccurredAt(updated.getOccurredAt());
        existing.setReporter(updated.getReporter());
        existing.setSeverity(updated.getSeverity());
        existing.setLatitude(updated.getLatitude());
        existing.setLongitude(updated.getLongitude());
        existing.setDiseaseName(updated.getDiseaseName());
        existing.setSuspectedAnimalSpecies(updated.getSuspectedAnimalSpecies());
        existing.setHumanCases(updated.getHumanCases());
        existing.setHumanDeaths(updated.getHumanDeaths());
        existing.setAnimalsAffected(updated.getAnimalsAffected());
        existing.setOutbreakStatus(updated.getOutbreakStatus());

        existing.setStatus(IncidentStatus.PENDING); // resubmitted after edit

        return repository.save(existing);
    }

    // ---------- Delete ----------

    public void delete(Long id, String callerWard, String callerHazard) {
        enforceHazardScope(callerHazard);
        ZoonoticIncident existing = findById(id);
        enforceWardScope(existing.getWard(), callerWard);
        repository.delete(existing);
    }

    // ---------- Approval workflow (provincial supervisor for this hazard only) ----------

    public ZoonoticIncident approve(Long id, String reviewer, String callerHazard) {
        enforceHazardScope(callerHazard);
        ZoonoticIncident incident = findById(id);
        incident.setStatus(IncidentStatus.APPROVED);
        incident.setReviewedBy(reviewer);
        incident.setReviewedAt(LocalDateTime.now());
        ZoonoticIncident saved = repository.save(incident);
        // Best-effort notification of alert-service (email / WhatsApp / Telegram).
        // This fulfils the integration TODO in flood-service; it never blocks an approval.
        alertNotifier.notifyApproved(saved);
        return saved;
    }

    public ZoonoticIncident reject(Long id, String reviewer, String reason, String callerHazard) {
        enforceHazardScope(callerHazard);
        ZoonoticIncident incident = findById(id);
        incident.setStatus(IncidentStatus.REJECTED);
        incident.setReviewedBy(reviewer);
        incident.setReviewedAt(LocalDateTime.now());
        incident.setReviewNotes(reason);
        return repository.save(incident);
    }

    public ZoonoticIncident requestCorrections(Long id, String reviewer, String notes, String callerHazard) {
        enforceHazardScope(callerHazard);
        ZoonoticIncident incident = findById(id);
        incident.setStatus(IncidentStatus.CORRECTIONS_REQUESTED);
        incident.setReviewedBy(reviewer);
        incident.setReviewedAt(LocalDateTime.now());
        incident.setReviewNotes(notes);
        return repository.save(incident);
    }

    // ---------- Scoping rules ----------

    // FR-SCOPE-01: every account is bound to ONE hazard. Anything else is rejected at the door.
    private void enforceHazardScope(String callerHazard) {
        if (callerHazard == null || !callerHazard.equalsIgnoreCase("zoonotic")) {
            throw new ForbiddenOperationException(
                    "This account is not authorised for the zoonotic hazard");
        }
    }

    // FR-SCOPE-01: ward recorders may only touch records of their own ward.
    private void enforceWardScope(String recordWard, String callerWard) {
        if (callerWard == null || !callerWard.equalsIgnoreCase(recordWard)) {
            throw new ForbiddenOperationException(
                    "This account is not authorised to act on records for ward: " + recordWard);
        }
    }
}
