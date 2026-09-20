package org.example.zoonoticdiseaseservice.repository;

import org.example.zoonoticdiseaseservice.entity.ApprovalAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalAuditRepository
        extends JpaRepository<ApprovalAudit, Long> {

    List<ApprovalAudit> findByDiseaseIdOrderByChangedAtDesc(
            Long diseaseId);
}