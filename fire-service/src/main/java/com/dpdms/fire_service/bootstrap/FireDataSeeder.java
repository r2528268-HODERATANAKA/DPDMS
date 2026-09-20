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

/**
 * Seeds realistic Rushinga fire incidents for the demo. Reporters are the ward recorder
 * of the incident's own ward (ward4/11/12/13.fire - seeded by auth-service), so the demo
 * data respects the (ward, hazard) scope.
 */
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

        seed("Veld fire spreading from communal land",
                "Late-burning veld fire driven by wind towards homesteads; ~40 hectares burnt.",
                "Ward 4", "Mudzi", "Mashonaland East", -16.7462, 32.2712,
                LocalDateTime.now().minusDays(3), "ward4.fire",
                Severity.HIGH, IncidentStatus.APPROVED, "fire.supervisor",
                "Verified with the Forestry Commission ward monitor.",
                41.5, "Escaped land-clearing burn", 0, 0, 2, FireStatus.ACTIVE);

        seed("Grass fire near dip tank",
                "Small grass fire extinguished by the community before reaching the cattle pens.",
                "Ward 4", "Mudzi", "Mashonaland East", -16.7399, 32.2830,
                LocalDateTime.now().minusDays(7), "ward4.fire",
                Severity.LOW, IncidentStatus.APPROVED, "fire.supervisor",
                "Confirmed contained within one hour.",
                1.2, "Cigarette stub near dip tank", 0, 0, 0, FireStatus.CONTAINED);

        seed("Bush fire threatening school block",
                "Fire advancing towards Mupata Primary School fence line; pupils evacuated.",
                "Ward 11", "Mudzi", "Mashonaland East", -16.8122, 32.3098,
                LocalDateTime.now().minusDays(1), "ward11.fire",
                Severity.HIGH, IncidentStatus.PENDING, null, null,
                26.0, "Unknown - suspected arson", 2, 0, 1, FireStatus.ACTIVE);

        seed("Hay stack fire at farmstead",
                "Baled hay store ignited, likely hot exhaust; no injuries.",
                "Ward 12", "Mudzi", "Mashonaland East", -16.7654, 32.2501,
                LocalDateTime.now().minusDays(5), "ward12.fire",
                Severity.MEDIUM, IncidentStatus.CORRECTIONS_REQUESTED, "fire.supervisor",
                "Please confirm the estimated value of losses.",
                0.4, "Hot exhaust near hay bales", 0, 0, 3, FireStatus.CONTAINED);

        seed("Peat smouldering in vlei area",
                "Underground peat still smouldering two days after surface flames stopped.",
                "Ward 13", "Mudzi", "Mashonaland East", -16.8301, 32.3542,
                LocalDateTime.now().minusDays(2), "ward13.fire",
                Severity.MEDIUM, IncidentStatus.PENDING, null, null,
                8.0, "Lightning strike", 0, 0, 0, FireStatus.CONTAINED);

        log.info("Seeded {} fire incidents", repository.count());
    }

    private void seed(String title, String description, String ward, String district, String province,
                      double lat, double lon, LocalDateTime occurredAt, String reporter,
                      Severity severity, IncidentStatus status, String reviewedBy, String reviewNotes,
                      Double areaBurnedHectares, String suspectedCause,
                      Integer injuries, Integer fatalities, Integer structuresDestroyed,
                      FireStatus fireStatus) {
        FireIncident e = FireIncident.builder()
                .ward(ward)
                .district(district)
                .province(province)
                .occurredAt(occurredAt)
                .reporter(reporter)
                .severity(severity)
                .status(status)
                .latitude(lat)
                .longitude(lon)
                .areaBurnedHectares(areaBurnedHectares)
                .suspectedCause(suspectedCause)
                .injuries(injuries)
                .fatalities(fatalities)
                .structuresDestroyed(structuresDestroyed)
                .fireStatus(fireStatus)
                .reviewedBy(reviewedBy)
                .reviewNotes(reviewNotes)
                .reviewedAt(status == IncidentStatus.APPROVED ? LocalDateTime.now().minusDays(1) : null)
                .createdAt(LocalDateTime.now())
                .build();
        repository.save(e);
    }
}
