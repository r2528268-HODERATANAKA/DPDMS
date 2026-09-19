package com.dpdms.flood_service.service;

import com.dpdms.flood_service.exception.ForbiddenOperationException;
import com.dpdms.flood_service.exception.ResourceNotFoundException;
import com.dpdms.flood_service.model.FloodIncident;
import com.dpdms.flood_service.model.IncidentStatus;
import com.dpdms.flood_service.repository.FloodIncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FloodIncidentService {

    private final FloodIncidentRepository repository;

    // ---------- Create ----------
    // NOTE: once auth-service issues JWTs, callerWard/callerHazard will come from the token's
    // claims instead of being passed in — this signature just makes the scoping rule explicit for now.
    public FloodIncident create(FloodIncident incident, String callerWard, String callerHazard) {
        enforceHazardScope(callerHazard);
        enforceWardScope(incident.getWard(), callerWard);
        incident.setStatus(IncidentStatus.PENDING);
        incident.setCreatedAt(LocalDateTime.now());
        return repository.save(incident);
    }

    // ---------- Read ----------

    public List<FloodIncident> findAllApproved() {
        return repository.findByStatus(IncidentStatus.APPROVED);
    }

    public List<FloodIncident> findAllForWard(String ward) {
        return repository.findByWard(ward);
    }

    public FloodIncident findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flood incident " + id + " not found"));
    }

    // ---------- Update (recorder editing a record still in PENDING or sent back for corrections) ----------

    public FloodIncident update(Long id, FloodIncident updated, String callerWard, String callerHazard) {
        enforceHazardScope(callerHazard);
        FloodIncident existing = findById(id);
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
        existing.setPeakWaterLevelMetres(updated.getPeakWaterLevelMetres());
        existing.setRiverBasin(updated.getRiverBasin());
        existing.setHouseholdsDisplaced(updated.getHouseholdsDisplaced());
        existing.setAreaFloodedHectares(updated.getAreaFloodedHectares());
        existing.setInundationDurationDays(updated.getInundationDurationDays());
        existing.setStatus(IncidentStatus.PENDING); // resubmitted after edit

        return repository.save(existing);
    }

    // ---------- Delete ----------

    public void delete(Long id, String callerWard, String callerHazard) {
        enforceHazardScope(callerHazard);
        FloodIncident existing = findById(id);
        enforceWardScope(existing.getWard(), callerWard);
        repository.delete(existing);
    }

    // ---------- Approval workflow (provincial flood supervisor only) ----------

    public FloodIncident approve(Long id, String reviewer, String callerHazard) {
        enforceHazardScope(callerHazard);
        FloodIncident incident = findById(id);
        incident.setStatus(IncidentStatus.APPROVED);
        incident.setReviewedBy(reviewer);
        incident.setReviewedAt(LocalDateTime.now());
        return repository.save(incident);
        // TODO (integration week): call alert-service here if peakWaterLevelMetres exceeds the danger threshold
    }

    public FloodIncident reject(Long id, String reviewer, String reason, String callerHazard) {
        enforceHazardScope(callerHazard);
        FloodIncident incident = findById(id);
        incident.setStatus(IncidentStatus.REJECTED);
        incident.setReviewedBy(reviewer);
        incident.setReviewedAt(LocalDateTime.now());
        incident.setReviewNotes(reason);
        return repository.save(incident);
    }

    public FloodIncident requestCorrections(Long id, String reviewer, String notes, String callerHazard) {
        enforceHazardScope(callerHazard);
        FloodIncident incident = findById(id);
        incident.setStatus(IncidentStatus.CORRECTIONS_REQUESTED);
        incident.setReviewedBy(reviewer);
        incident.setReviewedAt(LocalDateTime.now());
        incident.setReviewNotes(notes);
        return repository.save(incident);
    }

    // ---------- Scoping rules ----------

    private void enforceHazardScope(String callerHazard) {
        if (callerHazard == null || !callerHazard.equalsIgnoreCase("flood")) {
            throw new ForbiddenOperationException(
                    "This account is not authorised for the flood hazard");
        }
    }

    private void enforceWardScope(String recordWard, String callerWard) {
        if (callerWard == null || !callerWard.equalsIgnoreCase(recordWard)) {
            throw new ForbiddenOperationException(
                    "This account is not authorised to act on records for ward: " + recordWard);
        }
    }
}
