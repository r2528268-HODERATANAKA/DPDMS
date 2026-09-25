package com.dpdms.drought_service.bootstrap;

import com.dpdms.drought_service.model.DroughtIncident;
import com.dpdms.drought_service.model.IncidentStatus;
import com.dpdms.drought_service.model.Severity;
import com.dpdms.drought_service.model.WaterSourceCondition;
import com.dpdms.drought_service.repository.DroughtIncidentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Seeds a few Rushinga drought incidents so the dashboard has data on first run. */
@Component
@RequiredArgsConstructor
public class DroughtDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DroughtDataSeeder.class);

    private final DroughtIncidentRepository repository;

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }

        seed("Ward 3", -16.70, 32.20, 180.0, 45, 60, 12, 22.5,
                WaterSourceCondition.STRESSED, Severity.HIGH, IncidentStatus.APPROVED,
                "ward3.drought", "drought.supervisor", "Confirmed with the ward extension officer");
        seed("Ward 3", -16.71, 32.22, 95.0, 21, 25, 4, 8.0,
                WaterSourceCondition.NORMAL, Severity.MEDIUM, IncidentStatus.PENDING,
                "ward3.drought", null, null);

        log.info("Seeded {} drought incidents", repository.count());
    }

    private void seed(String ward, double lat, double lon, double deficit, int dryDays,
                      int households, int livestock, double crop,
                      WaterSourceCondition water, Severity severity, IncidentStatus status,
                      String reporter, String reviewer, String notes) {
        repository.save(DroughtIncident.builder()
                .ward(ward).district("Mudzi").province("Mashonaland East")
                .occurredAt(LocalDateTime.now().minusDays(5)).reporter(reporter)
                .severity(severity).status(status)
                .latitude(lat).longitude(lon)
                .rainfallDeficitMm(deficit).consecutiveDryDays(dryDays)
                .affectedHouseholds(households).livestockDeaths(livestock).cropDamageHectares(crop)
                .waterSourceCondition(water)
                .reviewedBy(reviewer)
                .reviewedAt(reviewer != null ? LocalDateTime.now().minusDays(1) : null)
                .reviewNotes(notes)
                .build());
    }
}
