package com.dpdms.dashboard_service.service;

import com.dpdms.dashboard_service.client.HazardDataFetcher;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Aggregates the five hazard feeds into one summary the frontend can render.
@Service
public class DashboardService {

    private final HazardDataFetcher fetcher;

    public DashboardService(HazardDataFetcher fetcher) {
        this.fetcher = fetcher;
    }

    private static final List<String> HAZARDS =
            List.of("flood", "drought", "fire", "zoonotic", "mining");

    public Map<String, Object> summary() {
        List<Map<String, Object>> recent = new ArrayList<>();
        Map<String, Object> perHazard = new LinkedHashMap<>();

        for (String hazard : HAZARDS) {
            List<Map<String, Object>> approved = fetcher.fetchApproved(hazard);

            Map<String, Long> bySeverity = new LinkedHashMap<>();
            bySeverity.put("LOW", 0L);
            bySeverity.put("MEDIUM", 0L);
            bySeverity.put("HIGH", 0L);
            bySeverity.put("CRITICAL", 0L);
            for (Map<String, Object> incident : approved) {
                String severity = String.valueOf(incident.get("severity"));
                bySeverity.merge(severity, 1L, Long::sum);
                recent.add(incident);
            }

            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("approved", (long) approved.size());
            stats.put("bySeverity", bySeverity);
            perHazard.put(hazard, stats);
        }

        // newest first by occurredAt (string comparison works for ISO timestamps)
        recent.sort(Comparator.comparing(
                i -> String.valueOf(i.get("occurredAt")), Comparator.reverseOrder()));
        List<Map<String, Object>> latestFive = recent.subList(0, Math.min(5, recent.size()));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("generatedAt", java.time.LocalDateTime.now().toString());
        summary.put("hazards", perHazard);
        summary.put("totalApproved", recent.size());
        summary.put("recentApproved", latestFive);
        return summary;
    }

    public Map<String, Object> health() {
        Map<String, Object> health = new LinkedHashMap<>();
        for (String hazard : HAZARDS) {
            health.put(hazard, fetcher.isUp(hazard) ? "UP" : "DOWN");
        }
        return health;
    }
}
