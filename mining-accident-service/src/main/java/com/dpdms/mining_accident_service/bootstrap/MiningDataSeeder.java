package com.dpdms.mining_accident_service.bootstrap;

import com.dpdms.mining_accident_service.model.AccidentType;
import com.dpdms.mining_accident_service.model.IncidentStatus;
import com.dpdms.mining_accident_service.model.MineStatus;
import com.dpdms.mining_accident_service.model.MiningAccident;
import com.dpdms.mining_accident_service.model.Severity;
import com.dpdms.mining_accident_service.repository.MiningAccidentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Seeds a few Rushinga mining incidents so the dashboard has data on first run. */
@Component
@RequiredArgsConstructor
public class MiningDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MiningDataSeeder.class);

    private final MiningAccidentRepository repository;

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }

        seed("Ward 6", -16.66, 32.15, "Nyamapanda Pit", AccidentType.ROCKFALL, 4, 2,
                MineStatus.SUSPENDED, "Roof collapse at an artisanal shaft; two miners trapped then rescued.",
                Severity.HIGH, IncidentStatus.APPROVED, "ward6.mining", "mining.supervisor", "Inspected by the mines officer");
        seed("Ward 6", -16.67, 32.16, "Chidodo Mine", AccidentType.GAS_LEAK, 1, 1,
                MineStatus.SUSPENDED, "Gas detected underground; shift evacuated as a precaution.",
                Severity.MEDIUM, IncidentStatus.PENDING, "ward6.mining", null, null);

        log.info("Seeded {} mining incidents", repository.count());
    }

    private void seed(String ward, double lat, double lon, String mineName, AccidentType type,
                      int casualties, int rescued, MineStatus mineStatus, String description,
                      Severity severity, IncidentStatus status,
                      String reporter, String reviewer, String notes) {
        repository.save(MiningAccident.builder()
                .ward(ward).district("Mudzi").province("Mashonaland East")
                .occurredAt(LocalDateTime.now().minusDays(6)).reporter(reporter)
                .severity(severity).status(status)
                .latitude(lat).longitude(lon)
                .mineName(mineName).accidentType(type).casualties(casualties).rescued(rescued)
                .mineOperationalStatus(mineStatus).description(description)
                .reviewedBy(reviewer)
                .reviewedAt(reviewer != null ? LocalDateTime.now().minusDays(1) : null)
                .reviewNotes(notes)
                .build());
    }
}
