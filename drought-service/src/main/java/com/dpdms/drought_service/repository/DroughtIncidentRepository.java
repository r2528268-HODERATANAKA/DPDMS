package com.dpdms.drought_service.repository;

import com.dpdms.drought_service.model.DroughtIncident;
import com.dpdms.drought_service.model.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DroughtIncidentRepository extends JpaRepository<DroughtIncident, Long> {

    // Only APPROVED incidents may reach the dashboard/map — used by report-service and dashboard-service
    List<DroughtIncident> findByStatus(IncidentStatus status);

    // A ward recorder should only ever see records from their own ward
    List<DroughtIncident> findByWardAndStatus(String ward, IncidentStatus status);

    List<DroughtIncident> findByWard(String ward);

    List<DroughtIncident> findByDistrict(String district);
}
