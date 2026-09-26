package com.dpdms.alert_service.repository;

import com.dpdms.alert_service.model.AlertLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertLogRepository extends JpaRepository<AlertLog, Long> {

    // Newest first for the alert log page
    List<AlertLog> findAllByOrderBySentAtDesc();

    List<AlertLog> findByHazardOrderBySentAtDesc(String hazard);

    // Used by the scan: has this approved incident already been alerted?
    boolean existsByHazardAndIncidentId(String hazard, Long incidentId);
}
