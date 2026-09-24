package com.dpdms.mining_accident_service.service;

import com.dpdms.mining_accident_service.exception.ForbiddenOperationException;
import com.dpdms.mining_accident_service.exception.ResourceNotFoundException;
import com.dpdms.mining_accident_service.model.MiningAccident;
import com.dpdms.mining_accident_service.model.IncidentStatus;
import com.dpdms.mining_accident_service.repository.MiningAccidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MiningAccidentService {

    private final MiningAccidentRepository repository;
    private final AlertNotifier alertNotifier;

    // ---------- Create ----------
    // NOTE: once auth-service issues JWTs and the gateway decodes them, callerWard/callerHazard
    // will arrive from the token's claims instead of headers — the scoping rules stay the same.
    public MiningAccident create(MiningAccident incident, String callerWard, String callerHazard) {
        enforceHazardScope(callerHazard);
        enforceWardScope(incident.getWard(), callerWard);
        incident.setStatus(IncidentStatus.PENDING);
        incident.setCreatedAt(LocalDateTime.now());
        return repository.save(incident);
    }

    // ---------- Read ----------

    public List<MiningAccident> findAllApproved() {
        return repository.findByStatus(IncidentStatus.APPROVED);
    }

    public List<MiningAccident> findAllForWard(String ward) {
        return repository.findByWard(ward);
    }

    public MiningAccident findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mining accident incident " + id + " not found"));
    }

    // ---------- Update (recorder editing a record still in PENDING or sent back for corrections) ----------

    public MiningAccident update(Long id, MiningAccident updated, String callerWard, String callerHazard) {
        enforceHazardScope(callerHazard);
        MiningAccident existing = findById(id);
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
        existing.setMineName(updated.getMineName());
        existing.setAccidentType(updated.getAccidentType());
        existing.setCasualties(updated.getCasualties());
        existing.setRescued(updated.getRescued());
        existing.setMineOperationalStatus(updated.getMineOperationalStatus());
        existing.setDescription(updated.getDescription());

        existing.setStatus(IncidentStatus.PENDING); // resubmitted after edit

        return repository.save(existing);
    }

    // ---------- Delete ----------

    public void delete(Long id, String callerWard, String callerHazard) {
        enforceHazardScope(callerHazard);
        MiningAccident existing = findById(id);
        enforceWardScope(existing.getWard(), callerWard);
        repository.delete(existing);
    }

    // ---------- Approval workflow (provincial supervisor for this hazard only) ----------

    public MiningAccident approve(Long id, String reviewer, String callerHazard) {
        enforceHazardScope(callerHazard);
        MiningAccident incident = findById(id);
        incident.setStatus(IncidentStatus.APPROVED);
        incident.setReviewedBy(reviewer);
        incident.setReviewedAt(LocalDateTime.now());
        MiningAccident saved = repository.save(incident);
        // Best-effort notification of alert-service (email / WhatsApp / Telegram).
        // This fulfils the integration TODO in flood-service; it never blocks an approval.
        alertNotifier.notifyApproved(saved);
        return saved;
    }

    public MiningAccident reject(Long id, String reviewer, String reason, String callerHazard) {
        enforceHazardScope(callerHazard);
        MiningAccident incident = findById(id);
        incident.setStatus(IncidentStatus.REJECTED);
        incident.setReviewedBy(reviewer);
        incident.setReviewedAt(LocalDateTime.now());
        incident.setReviewNotes(reason);
        return repository.save(incident);
    }

    public MiningAccident requestCorrections(Long id, String reviewer, String notes, String callerHazard) {
        enforceHazardScope(callerHazard);
        MiningAccident incident = findById(id);
        incident.setStatus(IncidentStatus.CORRECTIONS_REQUESTED);
        incident.setReviewedBy(reviewer);
        incident.setReviewedAt(LocalDateTime.now());
        incident.setReviewNotes(notes);
        return repository.save(incident);
    }

    // Role-scoped feed for the web dashboard: recorders see only their own ward (all statuses),
    // supervisors/admins see everything for this hazard. No headers -> approved-only consumers
    // (dashboard/report services) keep using findAllApproved().
    public List<MiningAccident> findAllScoped(String callerRole, String callerWard, String callerHazard) {
        if (callerHazard != null && !callerHazard.isBlank()
                && !"*".equals(callerHazard) && !callerHazard.equalsIgnoreCase("mining")) {
            throw new ForbiddenOperationException("This account is not authorised for the mining hazard");
        }
        if ("WARD_RECORDER".equalsIgnoreCase(callerRole) && callerWard != null && !callerWard.isBlank()) {
            return repository.findByWard(callerWard);
        }
        return repository.findAll();
    }

    // ---------- Scoping rules ----------

    // FR-SCOPE-01: every account is bound to ONE hazard. Anything else is rejected at the door.
    private void enforceHazardScope(String callerHazard) {
        if (callerHazard == null || !callerHazard.equalsIgnoreCase("mining")) {
            throw new ForbiddenOperationException(
                    "This account is not authorised for the mining hazard");
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
