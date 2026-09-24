package com.dpdms.fire_service.repository;

import com.dpdms.fire_service.model.FireIncident;
import com.dpdms.fire_service.model.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FireIncidentRepository extends JpaRepository<FireIncident, Long> {

    // Only APPROVED incidents may reach the dashboard/map — used by report-service and dashboard-service
    List<FireIncident> findByStatus(IncidentStatus status);

    // A ward recorder should only ever see records from their own ward
    List<FireIncident> findByWardAndStatus(String ward, IncidentStatus status);

    List<FireIncident> findByWard(String ward);

    List<FireIncident> findByDistrict(String district);
}
