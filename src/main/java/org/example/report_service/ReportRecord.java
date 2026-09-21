package org.example.report_service;

import java.time.LocalDateTime;

public class ReportRecord {

    private Long id;
    private String hazard;
    private String ward;
    private String district;
    private String province;
    private LocalDateTime incidentDateTime;
    private String reporter;
    private String severity;
    private String status;
    private Double latitude;
    private Double longitude;

    // Zoonotic disease fields
    private String pathogenName;
    private String animalSpeciesAffected;
    private Integer confirmedHumanCases;
    private Integer confirmedAnimalCases;
    private String clusterOutbreakClassification;

    // Generic hazard-specific data
    private String description;
    private String incidentType;
    private String location;
    private String affectedArea;
    private String additionalInformation;

    public ReportRecord() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getHazard() {
        return hazard;
    }

    public void setHazard(String hazard) {
        this.hazard = hazard;
    }

    public String getWard() {
        return ward;
    }

    public void setWard(String ward) {
        this.ward = ward;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public LocalDateTime getIncidentDateTime() {
        return incidentDateTime;
    }

    public void setIncidentDateTime(LocalDateTime incidentDateTime) {
        this.incidentDateTime = incidentDateTime;
    }

    public String getReporter() {
        return reporter;
    }

    public void setReporter(String reporter) {
        this.reporter = reporter;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getPathogenName() {
        return pathogenName;
    }

    public void setPathogenName(String pathogenName) {
        this.pathogenName = pathogenName;
    }

    public String getAnimalSpeciesAffected() {
        return animalSpeciesAffected;
    }

    public void setAnimalSpeciesAffected(String animalSpeciesAffected) {
        this.animalSpeciesAffected = animalSpeciesAffected;
    }

    public Integer getConfirmedHumanCases() {
        return confirmedHumanCases;
    }

    public void setConfirmedHumanCases(Integer confirmedHumanCases) {
        this.confirmedHumanCases = confirmedHumanCases;
    }

    public Integer getConfirmedAnimalCases() {
        return confirmedAnimalCases;
    }

    public void setConfirmedAnimalCases(Integer confirmedAnimalCases) {
        this.confirmedAnimalCases = confirmedAnimalCases;
    }

    public String getClusterOutbreakClassification() {
        return clusterOutbreakClassification;
    }

    public void setClusterOutbreakClassification(String clusterOutbreakClassification) {
        this.clusterOutbreakClassification = clusterOutbreakClassification;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIncidentType() {
        return incidentType;
    }

    public void setIncidentType(String incidentType) {
        this.incidentType = incidentType;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getAffectedArea() {
        return affectedArea;
    }

    public void setAffectedArea(String affectedArea) {
        this.affectedArea = affectedArea;
    }

    public String getAdditionalInformation() {
        return additionalInformation;
    }

    public void setAdditionalInformation(String additionalInformation) {
        this.additionalInformation = additionalInformation;
    }
}