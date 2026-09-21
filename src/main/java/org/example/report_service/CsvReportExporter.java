package org.example.report_service;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CsvReportExporter {

    public String export(List<ReportRecord> records) {

        StringBuilder csv = new StringBuilder();

        csv.append("ID,Hazard,Ward,District,Province,Incident Date Time,")
                .append("Reporter,Severity,Status,Latitude,Longitude,")
                .append("Pathogen,Animal Species,Human Cases,Animal Cases,")
                .append("Cluster/Outbreak Classification\n");

        for (ReportRecord record : records) {

            csv.append(value(record.getId())).append(",");
            csv.append(value(record.getHazard())).append(",");
            csv.append(value(record.getWard())).append(",");
            csv.append(value(record.getDistrict())).append(",");
            csv.append(value(record.getProvince())).append(",");
            csv.append(value(record.getIncidentDateTime())).append(",");
            csv.append(value(record.getReporter())).append(",");
            csv.append(value(record.getSeverity())).append(",");
            csv.append(value(record.getStatus())).append(",");
            csv.append(value(record.getLatitude())).append(",");
            csv.append(value(record.getLongitude())).append(",");
            csv.append(value(record.getPathogenName())).append(",");
            csv.append(value(record.getAnimalSpeciesAffected())).append(",");
            csv.append(value(record.getConfirmedHumanCases())).append(",");
            csv.append(value(record.getConfirmedAnimalCases())).append(",");
            csv.append(value(record.getClusterOutbreakClassification()))
                    .append("\n");
        }

        return csv.toString();
    }

    private String value(Object value) {

        if (value == null) {
            return "";
        }

        String text = String.valueOf(value);

        // Escape commas, quotes and line breaks for valid CSV
        if (text.contains(",") ||
                text.contains("\"") ||
                text.contains("\n")) {

            text = text.replace("\"", "\"\"");
            return "\"" + text + "\"";
        }

        return text;
    }
}