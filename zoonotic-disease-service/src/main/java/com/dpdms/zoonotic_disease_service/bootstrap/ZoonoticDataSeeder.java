package com.dpdms.zoonotic_disease_service.bootstrap;

import com.dpdms.zoonotic_disease_service.model.IncidentStatus;
import com.dpdms.zoonotic_disease_service.model.OutbreakStatus;
import com.dpdms.zoonotic_disease_service.model.Severity;
import com.dpdms.zoonotic_disease_service.model.ZoonoticIncident;
import com.dpdms.zoonotic_disease_service.repository.ZoonoticIncidentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Seeds a few Rushinga zoonotic incidents so the dashboard has data on first run. */
@Component
@RequiredArgsConstructor
public class ZoonoticDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ZoonoticDataSeeder.class);

    private final ZoonoticIncidentRepository repository;

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }

        seed("Ward 5", -16.68, 32.18, "Anthrax", "Cattle", 3, 12, 1,
                OutbreakStatus.SUSPECTED, Severity.HIGH, IncidentStatus.APPROVED,
                "ward5.zoo", "zoo.supervisor", "Samples sent to the provincial vet lab");
        seed("Ward 5", -16.69, 32.19, "Rabies", "Dogs", 0, 4, 0,
                OutbreakStatus.CONFIRMED, Severity.MEDIUM, IncidentStatus.PENDING,
                "ward5.zoo", null, null);

        log.info("Seeded {} zoonotic incidents", repository.count());
    }

    private void seed(String ward, double lat, double lon, String disease, String species,
                      int humanCases, int animals, int deaths,
                      OutbreakStatus outbreak, Severity severity, IncidentStatus status,
                      String reporter, String reviewer, String notes) {
        repository.save(ZoonoticIncident.builder()
                .ward(ward).district("Mudzi").province("Mashonaland East")
                .occurredAt(LocalDateTime.now().minusDays(4)).reporter(reporter)
                .severity(severity).status(status)
                .latitude(lat).longitude(lon)
                .diseaseName(disease).suspectedAnimalSpecies(species)
                .humanCases(humanCases).animalsAffected(animals).humanDeaths(deaths)
                .outbreakStatus(outbreak)
                .reviewedBy(reviewer)
                .reviewedAt(reviewer != null ? LocalDateTime.now().minusDays(1) : null)
                .reviewNotes(notes)
                .build());
    }
}
