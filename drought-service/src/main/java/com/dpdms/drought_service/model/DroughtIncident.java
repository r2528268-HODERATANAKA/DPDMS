package com.dpdms.drought_service.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "drought_incidents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DroughtIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ---------- Shared incident metadata (must match the model used by every hazard service) ----------

    @NotBlank(message = "Ward is required")
    private String ward;

    @NotBlank(message = "District is required")
    private String district;

    @NotBlank(message = "Province is required")
    private String province;

    @NotNull(message = "Date and time of occurrence is required")
    private LocalDateTime occurredAt;

    @NotBlank(message = "Reporter name is required")
    private String reporter;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Severity is required")
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private IncidentStatus status = IncidentStatus.PENDING;

    // ---------- GPS coordinates, captured at ward level at point of entry ----------

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
    @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
    @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
    private Double longitude;

    // ---------- Drought-specific indicators ----------

    @NotNull(message = "Rainfall deficit against seasonal average (mm) is required")
    @PositiveOrZero
    private Double rainfallDeficitMm;

    @NotNull(message = "Consecutive dry days is required")
    @PositiveOrZero
    private Integer consecutiveDryDays;

    @NotNull(message = "Number of affected households is required")
    @PositiveOrZero
    private Integer affectedHouseholds;

    @NotNull(message = "Number of livestock deaths is required")
    @PositiveOrZero
    private Integer livestockDeaths;

    @NotNull(message = "Estimated crop damage (hectares) is required")
    @PositiveOrZero
    private Double cropDamageHectares;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private WaterSourceCondition waterSourceCondition = WaterSourceCondition.NORMAL;

    // ---------- Audit trail (who approved/rejected, when) ----------

    private String reviewedBy;

    private LocalDateTime reviewedAt;

    private String reviewNotes;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
