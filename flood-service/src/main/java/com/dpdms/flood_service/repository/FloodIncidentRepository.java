package com.dpdms.flood_service.repository;

import com.dpdms.flood_service.model.FloodIncident;
import com.dpdms.flood_service.model.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FloodIncidentRepository extends JpaRepository<FloodIncident, Long> {

    // Only APPROVED incidents may reach the dashboard/map — used by report-service and dashboard-service
    List<FloodIncident> findByStatus(IncidentStatus status);

    // A ward recorder should only ever see records from their own ward
    List<FloodIncident> findByWardAndStatus(String ward, IncidentStatus status);

    List<FloodIncident> findByWard(String ward);

    List<FloodIncident> findByDistrict(String district);
}
