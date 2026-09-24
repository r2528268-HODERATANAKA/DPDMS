package com.dpdms.report_service.service;

import com.dpdms.report_service.client.HazardDataClient;
import com.dpdms.report_service.report.ReportGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

// Orchestrates: fetch approved rows for one hazard -> pick the right
// ReportGenerator (csv/pdf/xlsx/docx) -> return the finished file bytes.
@Service
@RequiredArgsConstructor
public class ReportService {

    private final HazardDataClient client;
    private final List<ReportGenerator> generators; // Spring injects all four implementations

    public byte[] buildReport(String hazard, String format, HttpHeaders callerHeaders) {
        List<Map<String, Object>> rows = client.fetchApproved(hazard, callerHeaders);
        String title = "DPDMS " + hazard.toUpperCase()
                + " incident report (approved records only)";

        return generators.stream()
                .filter(g -> g.format().equalsIgnoreCase(format))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unsupported format: " + format + " (use csv, pdf, xlsx or docx)"))
                .build(rows, title);
    }

    public ReportGenerator generatorFor(String format) {
        return generators.stream()
                .filter(g -> g.format().equalsIgnoreCase(format))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unsupported format: " + format + " (use csv, pdf, xlsx or docx)"));
    }
}
