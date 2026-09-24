package com.dpdms.mining_accident_service.repository;

import com.dpdms.mining_accident_service.model.MiningAccident;
import com.dpdms.mining_accident_service.model.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MiningAccidentRepository extends JpaRepository<MiningAccident, Long> {

    // Only APPROVED incidents may reach the dashboard/map — used by report-service and dashboard-service
    List<MiningAccident> findByStatus(IncidentStatus status);

    // A ward recorder should only ever see records from their own ward
    List<MiningAccident> findByWardAndStatus(String ward, IncidentStatus status);

    List<MiningAccident> findByWard(String ward);

    List<MiningAccident> findByDistrict(String district);
}
