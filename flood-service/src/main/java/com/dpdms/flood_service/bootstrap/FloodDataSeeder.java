package com.dpdms.flood_service.bootstrap;

import com.dpdms.flood_service.model.FloodIncident;
import com.dpdms.flood_service.model.IncidentStatus;
import com.dpdms.flood_service.model.Severity;
import com.dpdms.flood_service.repository.FloodIncidentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Seeds a few Rushinga flood incidents so the dashboard has data on first run. */
@Component
@RequiredArgsConstructor
public class FloodDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(FloodDataSeeder.class);

    private final FloodIncidentRepository repository;

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }

        seed("Ward 1", -16.75, 32.30, 1.8, "Mudzi River", 120, 45.5, 3,
                Severity.HIGH, IncidentStatus.APPROVED, "ward1.flood", "flood.supervisor", "Verified on site");
        seed("Ward 1", -16.76, 32.31, 0.9, "Nyadire tributary", 40, 12.0, 1,
                Severity.MEDIUM, IncidentStatus.PENDING, "ward1.flood", null, null);

        log.info("Seeded {} flood incidents", repository.count());
    }

    private void seed(String ward, double lat, double lon, double peak, String river,
                      int households, double area, int days,
                      Severity severity, IncidentStatus status,
                      String reporter, String reviewer, String notes) {
        repository.save(FloodIncident.builder()
                .ward(ward).district("Mudzi").province("Mashonaland East")
                .occurredAt(LocalDateTime.now().minusDays(3)).reporter(reporter)
                .severity(severity).status(status)
                .latitude(lat).longitude(lon)
                .peakWaterLevelMetres(peak).riverBasin(river)
                .householdsDisplaced(households).areaFloodedHectares(area).inundationDurationDays(days)
                .reviewedBy(reviewer)
                .reviewedAt(reviewer != null ? LocalDateTime.now().minusDays(1) : null)
                .reviewNotes(notes)
                .build());
    }
}
