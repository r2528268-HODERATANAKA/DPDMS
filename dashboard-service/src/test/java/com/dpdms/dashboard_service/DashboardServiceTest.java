package com.dpdms.dashboard_service;

import com.dpdms.dashboard_service.client.HazardDataFetcher;
import com.dpdms.dashboard_service.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private HazardDataFetcher fetcher;

    @InjectMocks
    private DashboardService service;

    private void feedFiveEmpty() {
        when(fetcher.fetchApproved("drought")).thenReturn(List.of());
        when(fetcher.fetchApproved("fire")).thenReturn(List.of());
        when(fetcher.fetchApproved("zoonotic")).thenReturn(List.of());
        when(fetcher.fetchApproved("mining")).thenReturn(List.of());
    }

    @Test
    void summaryCountsApprovedPerHazardAndBySeverity() {
        when(fetcher.fetchApproved("flood")).thenReturn(List.of(
                Map.of("id", 1, "ward", "Mudzi", "severity", "HIGH", "occurredAt", "2026-09-20T10:00:00"),
                Map.of("id", 2, "ward", "Kanyemba", "severity", "CRITICAL", "occurredAt", "2026-09-21T10:00:00"),
                Map.of("id", 3, "ward", "Mudzi", "severity", "HIGH", "occurredAt", "2026-09-19T10:00:00")));
        feedFiveEmpty();

        Map<String, Object> summary = service.summary();

        assertEquals(3, summary.get("totalApproved"));
        var floods = (Map<String, Object>) ((Map<?, ?>) summary.get("hazards")).get("flood");
        assertEquals(3L, floods.get("approved"));
        var bySeverity = (Map<String, Long>) floods.get("bySeverity");
        assertEquals(2L, bySeverity.get("HIGH"));
        assertEquals(1L, bySeverity.get("CRITICAL"));
    }

    @Test
    void recentApprovedIsNewestFirstAndCappedAtFive() {
        when(fetcher.fetchApproved("flood")).thenReturn(List.of(
                Map.of("id", 1, "severity", "LOW", "occurredAt", "2026-09-01T10:00:00"),
                Map.of("id", 2, "severity", "HIGH", "occurredAt", "2026-09-22T10:00:00"),
                Map.of("id", 3, "severity", "MEDIUM", "occurredAt", "2026-09-15T10:00:00"),
                Map.of("id", 4, "severity", "HIGH", "occurredAt", "2026-09-10T10:00:00"),
                Map.of("id", 5, "severity", "LOW", "occurredAt", "2026-09-05T10:00:00"),
                Map.of("id", 6, "severity", "CRITICAL", "occurredAt", "2026-09-21T10:00:00")));
        feedFiveEmpty();

        Map<String, Object> summary = service.summary();
        List<Map<String, Object>> recent =
                (List<Map<String, Object>>) summary.get("recentApproved");

        assertEquals(5, recent.size());                     // capped at 5
        assertEquals(2, recent.get(0).get("id"));           // newest first
    }

    @Test
    void deadHazardServiceDoesNotBreakTheDashboard() {
        when(fetcher.fetchApproved("flood")).thenReturn(List.of()); // fetcher returns empty for dead services
        feedFiveEmpty();

        Map<String, Object> summary = service.summary();

        assertEquals(0, summary.get("totalApproved"));
    }

    @Test
    void healthReportsUpDownPerHazard() {
        when(fetcher.isUp("flood")).thenReturn(true);
        when(fetcher.isUp("drought")).thenReturn(false);
        when(fetcher.isUp("fire")).thenReturn(true);
        when(fetcher.isUp("zoonotic")).thenReturn(true);
        when(fetcher.isUp("mining")).thenReturn(true);

        Map<String, Object> health = service.health();

        assertEquals("UP", health.get("flood"));
        assertEquals("DOWN", health.get("drought"));
    }
}
