package com.dpdms.fire_service.repository;

import com.dpdms.fire_service.model.FireIncident;
import com.dpdms.fire_service.model.FireStatus;
import com.dpdms.fire_service.model.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FireIncidentRepository extends JpaRepository<FireIncident, Long> {

    /** Approved-only feed used by the dashboard/report services. */
    List<FireIncident> findByStatus(IncidentStatus status);

    /** A ward recorder's own ward view. */
    List<FireIncident> findByWard(String ward);

    List<FireIncident> findByWardAndStatus(String ward, IncidentStatus status);

    List<FireIncident> findByDistrict(String district);

    /** Active fires - what the alert/dashboard services need for the live map. */
    List<FireIncident> findByFireStatus(FireStatus fireStatus);
}
