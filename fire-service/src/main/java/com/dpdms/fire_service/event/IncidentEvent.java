package com.dpdms.fire_service.event;

import java.io.Serializable;
import java.util.List;

/**
 * The incident event published to the RabbitMQ topic exchange "incident.events" with
 * routing key "incident.approved.{hazard}". alert-service consumes this and fans out
 * Email + WhatsApp notifications. Simple types only, so the JSON payload is portable.
 */
public class IncidentEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventId;
    private Long incidentId;
    private String hazardType;
    private String ward;
    private String district;
    private String province;
    private String severity;
    private int severityRank;
    private Double latitude;
    private Double longitude;
    private String occurredAt;
    private String reporter;
    private String approvedBy;
    private String approvedAt;
    private List<String> recommendedActions;

    public IncidentEvent() {
        // required for JSON deserialisation
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public Long getIncidentId() { return incidentId; }
    public void setIncidentId(Long incidentId) { this.incidentId = incidentId; }

    public String getHazardType() { return hazardType; }
    public void setHazardType(String hazardType) { this.hazardType = hazardType; }

    public String getWard() { return ward; }
    public void setWard(String ward) { this.ward = ward; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public int getSeverityRank() { return severityRank; }
    public void setSeverityRank(int severityRank) { this.severityRank = severityRank; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getOccurredAt() { return occurredAt; }
    public void setOccurredAt(String occurredAt) { this.occurredAt = occurredAt; }

    public String getReporter() { return reporter; }
    public void setReporter(String reporter) { this.reporter = reporter; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public String getApprovedAt() { return approvedAt; }
    public void setApprovedAt(String approvedAt) { this.approvedAt = approvedAt; }

    public List<String> getRecommendedActions() { return recommendedActions; }
    public void setRecommendedActions(List<String> recommendedActions) {
        this.recommendedActions = recommendedActions;
    }
}
