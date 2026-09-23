package com.dpdms.zoonotic_disease_service.repository;

import com.dpdms.zoonotic_disease_service.model.ZoonoticIncident;
import com.dpdms.zoonotic_disease_service.model.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ZoonoticIncidentRepository extends JpaRepository<ZoonoticIncident, Long> {

    // Only APPROVED incidents may reach the dashboard/map — used by report-service and dashboard-service
    List<ZoonoticIncident> findByStatus(IncidentStatus status);

    // A ward recorder should only ever see records from their own ward
    List<ZoonoticIncident> findByWardAndStatus(String ward, IncidentStatus status);

    List<ZoonoticIncident> findByWard(String ward);

    List<ZoonoticIncident> findByDistrict(String district);
}
