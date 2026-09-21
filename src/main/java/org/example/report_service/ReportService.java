package org.example.report_service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final ZoonoticDiseaseClient hazardReportClient;

    public ReportService(ZoonoticDiseaseClient hazardReportClient) {
        this.hazardReportClient = hazardReportClient;
    }

    public List<ReportRecord> getAllApprovedRecords() {

        return hazardReportClient.getAllHazardReports()
                .stream()
                .filter(record ->
                        "APPROVED".equalsIgnoreCase(record.getStatus()))
                .collect(Collectors.toList());
    }

    public List<ReportRecord> filterRecords(
            String hazard,
            String ward,
            String district,
            LocalDate startDate,
            LocalDate endDate,
            String severity,
            String status) {

        return hazardReportClient.getAllHazardReports()
                .stream()

                // Only approved incidents appear in reports.
                .filter(record ->
                        "APPROVED".equalsIgnoreCase(record.getStatus()))

                .filter(record ->
                        hazard == null ||
                                hazard.isBlank() ||
                                hazard.equalsIgnoreCase(record.getHazard()))

                .filter(record ->
                        ward == null ||
                                ward.isBlank() ||
                                ward.equalsIgnoreCase(record.getWard()))

                .filter(record ->
                        district == null ||
                                district.isBlank() ||
                                district.equalsIgnoreCase(record.getDistrict()))

                .filter(record ->
                        severity == null ||
                                severity.isBlank() ||
                                severity.equalsIgnoreCase(record.getSeverity()))

                .filter(record ->
                        status == null ||
                                status.isBlank() ||
                                status.equalsIgnoreCase(record.getStatus()))

                .filter(record ->
                        startDate == null ||
                                record.getIncidentDateTime() == null ||
                                !record.getIncidentDateTime()
                                        .toLocalDate()
                                        .isBefore(startDate))

                .filter(record ->
                        endDate == null ||
                                record.getIncidentDateTime() == null ||
                                !record.getIncidentDateTime()
                                        .toLocalDate()
                                        .isAfter(endDate))

                .collect(Collectors.toList());
    }
}