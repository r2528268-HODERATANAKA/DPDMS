package com.dpdms.fire_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A fire incident (veld and structural fires).
 *
 * Shared metadata contract (identical to flood-service and the other hazards):
 *   ward, district, province, occurredAt, reporter, severity, status, GPS lat/lng, audit trail.
 *
 * Five fire indicators from the assignment brief:
 *   1. areaBurnedHectares   2. suspectedCause   3. injuries + fatalities
 *   4. structuresDestroyed  5. fireStatus (ACTIVE | CONTAINED)
 */
@Entity
@Table(name = "fire_incidents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FireIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ---------- Shared incident metadata ----------

    @NotBlank(message = "Ward is required")
    @Column(nullable = false, length = 60)
    private String ward;

    @NotBlank(message = "District is required")
    private String district;

    @NotBlank(message = "Province is required")
    private String province;

    @NotNull(message = "Date and time of occurrence is required")
    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @NotBlank(message = "Reporter name is required")
    private String reporter;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Severity is required")
    @Column(nullable = false, length = 20)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 30)
    private IncidentStatus status = IncidentStatus.PENDING;

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
    @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
    @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
    private Double longitude;

    // ---------- The five fire indicators ----------

    @NotNull(message = "Estimated area burned (hectares) is required")
    @PositiveOrZero(message = "Area burned cannot be negative")
    @Column(name = "area_burned_hectares")
    private Double areaBurnedHectares;

    @NotBlank(message = "Suspected cause of the fire is required")
    @Column(length = 200)
    private String suspectedCause;

    @NotNull(message = "Number of injuries is required (0 if none)")
    @PositiveOrZero(message = "Injuries cannot be negative")
    private Integer injuries;

    @NotNull(message = "Number of fatalities is required (0 if none)")
    @PositiveOrZero(message = "Fatalities cannot be negative")
    private Integer fatalities;

    @NotNull(message = "Number of structures destroyed is required (0 if none)")
    @PositiveOrZero(message = "Structures destroyed cannot be negative")
    @Column(name = "structures_destroyed")
    private Integer structuresDestroyed;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Fire status (ACTIVE or CONTAINED) is required")
    @Column(name = "fire_status", nullable = false, length = 20)
    private FireStatus fireStatus;

    // ---------- Audit trail ----------

    @Column(name = "reviewed_by", length = 120)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_notes", length = 1000)
    private String reviewNotes;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // ---------- Encapsulated workflow (state-machine enforcement) ----------

    /**
     * Apply a review decision. Only legal transitions are accepted; anything else throws
     * IllegalStateException, which the web layer maps to HTTP 409.
     */
    public void applyReview(IncidentStatus decision, String reviewer, String comment) {
        if (!this.status.canTransitionTo(decision)) {
            throw new IllegalStateException(
                    "Illegal transition: a " + this.status + " incident cannot become " + decision);
        }
        this.status = decision;
        this.reviewedBy = reviewer;
        this.reviewedAt = LocalDateTime.now();
        this.reviewNotes = comment;
    }

    /** @return true when the record may still be edited by its recorder. */
    public boolean isEditable() {
        return this.status == IncidentStatus.PENDING
                || this.status == IncidentStatus.CORRECTIONS_REQUESTED;
    }

    /** @return the hazard slug used on the event/alert topics (also exposed in JSON). */
    public String getHazardType() {
        return "fire";
    }
}
