package com.dpdms.fire_service.bootstrap;

import com.dpdms.fire_service.model.FireIncident;
import com.dpdms.fire_service.model.FireStatus;
import com.dpdms.fire_service.model.IncidentStatus;
import com.dpdms.fire_service.model.Severity;
import com.dpdms.fire_service.repository.FireIncidentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Seeds a few Rushinga fire incidents so the dashboard has data on first run. */
@Component
@RequiredArgsConstructor
public class FireDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(FireDataSeeder.class);

    private final FireIncidentRepository repository;

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }

        seed("Ward 4", "Mudzi", -16.75, 32.28, 41.5, "Escaped land-clearing burn",
                0, 0, 2, FireStatus.ACTIVE, Severity.HIGH, IncidentStatus.APPROVED,
                "ward4.fire", "fire.supervisor", "Verified with the ward monitor");
        seed("Ward 4", "Mudzi", -16.74, 32.29, 1.2, "Cigarette stub near dip tank",
                0, 0, 0, FireStatus.CONTAINED, Severity.LOW, IncidentStatus.APPROVED,
                "ward4.fire", "fire.supervisor", "Contained within the hour");
        seed("Ward 11", "Mudzi", -16.81, 32.31, 26.0, "Unknown - suspected arson",
                2, 0, 1, FireStatus.ACTIVE, Severity.HIGH, IncidentStatus.PENDING,
                "ward11.fire", null, null);
        seed("Ward 12", "Mudzi", -16.77, 32.25, 0.4, "Hot exhaust near hay bales",
                0, 0, 3, FireStatus.CONTAINED, Severity.MEDIUM, IncidentStatus.CORRECTIONS_REQUESTED,
                "ward12.fire", "fire.supervisor", "Please confirm the value of losses");
        seed("Ward 13", "Mudzi", -16.83, 32.35, 8.0, "Lightning strike",
                0, 0, 0, FireStatus.CONTAINED, Severity.MEDIUM, IncidentStatus.PENDING,
                "ward13.fire", null, null);

        log.info("Seeded {} fire incidents", repository.count());
    }

    private void seed(String ward, String district, double lat, double lon,
                      double area, String cause, int injuries, int fatalities, int structures,
                      FireStatus fireStatus, Severity severity, IncidentStatus status,
                      String reporter, String reviewer, String notes) {
        repository.save(FireIncident.builder()
                .ward(ward).district(district).province("Mashonaland East")
                .occurredAt(LocalDateTime.now().minusDays(2)).reporter(reporter)
                .severity(severity).status(status)
                .latitude(lat).longitude(lon)
                .areaBurnedHectares(area).suspectedCause(cause)
                .injuries(injuries).fatalities(fatalities).structuresDestroyed(structures)
                .fireStatus(fireStatus)
                .reviewedBy(reviewer)
                .reviewedAt(reviewer != null ? LocalDateTime.now().minusDays(1) : null)
                .reviewNotes(notes)
                .build());
    }
}
