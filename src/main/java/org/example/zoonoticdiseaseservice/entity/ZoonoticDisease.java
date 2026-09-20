package org.example.zoonoticdiseaseservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "zoonotic_diseases")
public class ZoonoticDisease {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String hazard;

    @NotBlank(message = "Ward is required")
    private String ward;

    @NotBlank(message = "District is required")
    private String district;

    @NotBlank(message = "Province is required")
    private String province;

    @NotNull(message = "Incident date and time is required")
    private LocalDateTime incidentDateTime;

    @NotBlank(message = "Reporter is required")
    private String reporter;

    @NotBlank(message = "Severity is required")
    private String severity;

    private String status = "PENDING";

    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private Double longitude;

    @NotBlank(message = "Pathogen name is required")
    private String pathogenName;

    @NotBlank(message = "Animal species affected is required")
    private String animalSpeciesAffected;

    @Min(value = 0, message = "Confirmed human cases cannot be negative")
    private Integer confirmedHumanCases;

    @Min(value = 0, message = "Confirmed animal cases cannot be negative")
    private Integer confirmedAnimalCases;

    @NotBlank(message = "Cluster/outbreak classification is required")
    private String clusterOutbreakClassification;

    private String approvalReason;

    public ZoonoticDisease() {
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

    public String getApprovalReason() {
        return approvalReason;
    }

    public void setApprovalReason(String approvalReason) {
        this.approvalReason = approvalReason;
    }
}