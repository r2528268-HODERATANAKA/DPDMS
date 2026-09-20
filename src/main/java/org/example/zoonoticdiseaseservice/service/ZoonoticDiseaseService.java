package org.example.zoonoticdiseaseservice.service;

import org.example.zoonoticdiseaseservice.entity.ApprovalAudit;
import org.example.zoonoticdiseaseservice.entity.ZoonoticDisease;
import org.example.zoonoticdiseaseservice.repository.ApprovalAuditRepository;
import org.example.zoonoticdiseaseservice.repository.ZoonoticDiseaseRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ZoonoticDiseaseService {

    private final ZoonoticDiseaseRepository repository;
    private final ApprovalAuditRepository auditRepository;

    public ZoonoticDiseaseService(
            ZoonoticDiseaseRepository repository,
            ApprovalAuditRepository auditRepository) {

        this.repository = repository;
        this.auditRepository = auditRepository;
    }

    // GET ALL
    public List<ZoonoticDisease> getAllDiseases() {
        return repository.findAll();
    }

    // GET BY ID
    public Optional<ZoonoticDisease> getDiseaseById(Long id) {
        return repository.findById(id);
    }

    // CREATE
    public ZoonoticDisease createDisease(ZoonoticDisease disease) {

        disease.setHazard("ZOONOTIC_DISEASE");
        disease.setStatus("PENDING");
        disease.setApprovalReason(null);

        ZoonoticDisease savedDisease = repository.save(disease);

        saveAudit(
                savedDisease.getId(),
                null,
                "PENDING",
                "Record submitted",
                savedDisease.getReporter()
        );

        return savedDisease;
    }

    // UPDATE
    public ZoonoticDisease updateDisease(
            Long id,
            ZoonoticDisease updatedDisease) {

        return repository.findById(id)
                .map(existingDisease -> {

                    String previousStatus =
                            existingDisease.getStatus();

                    existingDisease.setWard(
                            updatedDisease.getWard());
                    existingDisease.setDistrict(
                            updatedDisease.getDistrict());
                    existingDisease.setProvince(
                            updatedDisease.getProvince());
                    existingDisease.setIncidentDateTime(
                            updatedDisease.getIncidentDateTime());
                    existingDisease.setReporter(
                            updatedDisease.getReporter());
                    existingDisease.setSeverity(
                            updatedDisease.getSeverity());
                    existingDisease.setLatitude(
                            updatedDisease.getLatitude());
                    existingDisease.setLongitude(
                            updatedDisease.getLongitude());
                    existingDisease.setPathogenName(
                            updatedDisease.getPathogenName());
                    existingDisease.setAnimalSpeciesAffected(
                            updatedDisease.getAnimalSpeciesAffected());
                    existingDisease.setConfirmedHumanCases(
                            updatedDisease.getConfirmedHumanCases());
                    existingDisease.setConfirmedAnimalCases(
                            updatedDisease.getConfirmedAnimalCases());
                    existingDisease.setClusterOutbreakClassification(
                            updatedDisease
                                    .getClusterOutbreakClassification());

                    existingDisease.setHazard(
                            "ZOONOTIC_DISEASE");

                    existingDisease.setStatus("PENDING");
                    existingDisease.setApprovalReason(null);

                    ZoonoticDisease savedDisease =
                            repository.save(existingDisease);

                    saveAudit(
                            id,
                            previousStatus,
                            "PENDING",
                            "Record updated and resubmitted",
                            savedDisease.getReporter()
                    );

                    return savedDisease;
                })
                .orElseThrow(() ->
                        new RuntimeException(
                                "Disease record not found"));
    }

    // DELETE
    public void deleteDisease(Long id) {

        if (!repository.existsById(id)) {
            throw new RuntimeException(
                    "Disease record not found");
        }

        repository.deleteById(id);
    }

    // APPROVE
    public ZoonoticDisease approveDisease(Long id) {

        return repository.findById(id)
                .map(disease -> {

                    String previousStatus =
                            disease.getStatus();

                    disease.setStatus("APPROVED");
                    disease.setApprovalReason(null);

                    ZoonoticDisease savedDisease =
                            repository.save(disease);

                    saveAudit(
                            id,
                            previousStatus,
                            "APPROVED",
                            "Record approved",
                            "SUPERVISOR"
                    );

                    return savedDisease;
                })
                .orElseThrow(() ->
                        new RuntimeException(
                                "Disease record not found"));
    }

    // REJECT
    public ZoonoticDisease rejectDisease(
            Long id,
            String reason) {

        return repository.findById(id)
                .map(disease -> {

                    String previousStatus =
                            disease.getStatus();

                    disease.setStatus("REJECTED");
                    disease.setApprovalReason(reason);

                    ZoonoticDisease savedDisease =
                            repository.save(disease);

                    saveAudit(
                            id,
                            previousStatus,
                            "REJECTED",
                            reason,
                            "SUPERVISOR"
                    );

                    return savedDisease;
                })
                .orElseThrow(() ->
                        new RuntimeException(
                                "Disease record not found"));
    }

    // REQUEST CORRECTIONS
    public ZoonoticDisease requestCorrections(
            Long id,
            String reason) {

        return repository.findById(id)
                .map(disease -> {

                    String previousStatus =
                            disease.getStatus();

                    disease.setStatus(
                            "CORRECTION_REQUESTED");

                    disease.setApprovalReason(reason);

                    ZoonoticDisease savedDisease =
                            repository.save(disease);

                    saveAudit(
                            id,
                            previousStatus,
                            "CORRECTION_REQUESTED",
                            reason,
                            "SUPERVISOR"
                    );

                    return savedDisease;
                })
                .orElseThrow(() ->
                        new RuntimeException(
                                "Disease record not found"));
    }

    // SAVE AUDIT ENTRY
    private void saveAudit(
            Long diseaseId,
            String previousStatus,
            String newStatus,
            String reason,
            String changedBy) {

        ApprovalAudit audit = new ApprovalAudit();

        audit.setDiseaseId(diseaseId);
        audit.setPreviousStatus(previousStatus);
        audit.setNewStatus(newStatus);
        audit.setReason(reason);
        audit.setChangedBy(changedBy);
        audit.setChangedAt(LocalDateTime.now());

        auditRepository.save(audit);
    }

    // GET AUDIT HISTORY
    public List<ApprovalAudit> getAuditHistory(
            Long diseaseId) {

        return auditRepository
                .findByDiseaseIdOrderByChangedAtDesc(
                        diseaseId);
    }
}