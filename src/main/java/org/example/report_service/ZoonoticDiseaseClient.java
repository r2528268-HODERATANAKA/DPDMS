package org.example.report_service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class ZoonoticDiseaseClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String ZOONOTIC_SERVICE_URL =
            "http://localhost:8085/api/zoonotic-diseases";

    private static final String FLOOD_SERVICE_URL =
            "http://localhost:8081/api/floods";

    private static final String FIRE_SERVICE_URL =
            "http://localhost:8203/api/v1/fire/incidents";

    public ZoonoticDiseaseClient() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public List<ReportRecord> getAllHazardReports() {

        List<ReportRecord> records = new ArrayList<>();

        records.addAll(getRecords(ZOONOTIC_SERVICE_URL));
        records.addAll(getRecords(FLOOD_SERVICE_URL));
        records.addAll(getRecords(FIRE_SERVICE_URL));

        return records;
    }

    public List<ReportRecord> getZoonoticDiseases() {
        return getRecords(ZOONOTIC_SERVICE_URL);
    }

    private List<ReportRecord> getRecords(String url) {

        try {
            String response = restTemplate.getForObject(
                    url,
                    String.class
            );

            if (response == null || response.isBlank()) {
                return Collections.emptyList();
            }

            JsonNode root = objectMapper.readTree(response);

            List<ReportRecord> records = new ArrayList<>();

            if (root.isArray()) {
                for (JsonNode node : root) {
                    records.add(convertToReportRecord(node));
                }
            } else if (root.isObject()) {
                JsonNode data = root.get("data");

                if (data != null && data.isArray()) {
                    for (JsonNode node : data) {
                        records.add(convertToReportRecord(node));
                    }
                } else {
                    records.add(convertToReportRecord(root));
                }
            }

            return records;

        } catch (Exception e) {
            System.out.println(
                    "Could not retrieve reports from " + url +
                            ": " + e.getMessage()
            );

            return Collections.emptyList();
        }
    }

    private ReportRecord convertToReportRecord(JsonNode node) {

        ReportRecord record = new ReportRecord();

        record.setId(getLong(node, "id"));
        record.setHazard(getText(node, "hazard", "hazardType", "hazard_type"));
        record.setWard(getText(node, "ward"));
        record.setDistrict(getText(node, "district"));
        record.setProvince(getText(node, "province"));
        record.setReporter(getText(node, "reporter", "reportedBy", "reported_by"));
        record.setSeverity(getText(node, "severity"));
        record.setStatus(getText(node, "status", "approvalStatus", "approval_status"));

        record.setLatitude(getDouble(node, "latitude", "lat"));
        record.setLongitude(getDouble(node, "longitude", "lng", "lon", "long"));

        record.setIncidentDateTime(
                getDateTime(
                        node,
                        "incidentDateTime",
                        "incident_datetime",
                        "dateTime",
                        "date_time",
                        "reportedAt",
                        "reported_at"
                )
        );

        record.setPathogenName(
                getText(node, "pathogenName", "pathogen_name", "diseaseName", "disease_name")
        );

        record.setAnimalSpeciesAffected(
                getText(
                        node,
                        "animalSpeciesAffected",
                        "animal_species_affected",
                        "animalSpecies",
                        "animal_species"
                )
        );

        record.setConfirmedHumanCases(
                getInteger(node, "confirmedHumanCases", "confirmed_human_cases")
        );

        record.setConfirmedAnimalCases(
                getInteger(node, "confirmedAnimalCases", "confirmed_animal_cases")
        );

        record.setClusterOutbreakClassification(
                getText(
                        node,
                        "clusterOutbreakClassification",
                        "cluster_outbreak_classification",
                        "outbreakClassification",
                        "outbreak_classification"
                )
        );

        return record;
    }

    private String getText(JsonNode node, String... names) {

        for (String name : names) {

            JsonNode value = node.get(name);

            if (value != null && !value.isNull()) {
                return value.asText();
            }
        }

        return null;
    }

    private Long getLong(JsonNode node, String... names) {

        for (String name : names) {

            JsonNode value = node.get(name);

            if (value != null && value.isNumber()) {
                return value.asLong();
            }
        }

        return null;
    }

    private Integer getInteger(JsonNode node, String... names) {

        for (String name : names) {

            JsonNode value = node.get(name);

            if (value != null && value.isNumber()) {
                return value.asInt();
            }
        }

        return null;
    }

    private Double getDouble(JsonNode node, String... names) {

        for (String name : names) {

            JsonNode value = node.get(name);

            if (value != null && value.isNumber()) {
                return value.asDouble();
            }
        }

        return null;
    }

    private LocalDateTime getDateTime(
            JsonNode node,
            String... names) {

        String value = getText(node, names);

        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalDateTime.parse(value);
        } catch (Exception e) {
            return null;
        }
    }
}