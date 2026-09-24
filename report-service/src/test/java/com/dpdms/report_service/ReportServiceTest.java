package com.dpdms.report_service;

import com.dpdms.report_service.client.HazardDataClient;
import com.dpdms.report_service.report.CsvReportGenerator;
import com.dpdms.report_service.report.PdfReportGenerator;
import com.dpdms.report_service.service.ReportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private HazardDataClient client;

    private final HttpHeaders headers = new HttpHeaders();

    @Test
    void buildReportUsesFetchedRowsInCsvFormat() {
        when(client.fetchApproved(eq("fire"), any(HttpHeaders.class))).thenReturn(List.of(
                Map.of("id", 1, "ward", "Mudzi", "severity", "HIGH", "status", "APPROVED")));

        ReportService service = new ReportService(client,
                List.of(new CsvReportGenerator(), new PdfReportGenerator()));

        byte[] bytes = service.buildReport("fire", "csv", headers);
        String csv = new String(bytes, StandardCharsets.UTF_8);
        assertTrue(csv.contains("Mudzi"));
        assertTrue(csv.contains("severity"));
    }

    @Test
    void unknownFormatIsRejectedWithClearMessage() {
        ReportService service = new ReportService(client,
                List.of(new CsvReportGenerator(), new PdfReportGenerator()));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.generatorFor("xml"));
        assertTrue(ex.getMessage().contains("xml"));
    }

    @Test
    void unknownHazardIsRejectedByTheClient() {
        when(client.fetchApproved(eq("storm"), any(HttpHeaders.class)))
                .thenThrow(new IllegalArgumentException("Unknown hazard: storm"));

        ReportService service = new ReportService(client,
                List.of(new CsvReportGenerator(), new PdfReportGenerator()));

        assertThrows(IllegalArgumentException.class,
                () -> service.buildReport("storm", "csv", headers));
    }
}
