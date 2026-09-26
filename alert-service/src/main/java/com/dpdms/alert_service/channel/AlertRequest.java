package com.dpdms.alert_service.channel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// The payload that travels through the channels.
// Built either by AlertController (manual demo alert) or by AlertService
// when it scans the hazard services for newly approved incidents.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRequest {

    private String hazard;       // "flood", "drought", "fire", "zoonotic", "mining"
    private Long incidentId;     // null for manual test alerts
    private String ward;
    private String district;
    private String severity;     // LOW / MEDIUM / HIGH / CRITICAL
    private String message;      // human-readable alert text
}
