package com.dpdms.alert_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// One row per delivery attempt: "incident #12 approved -> EMAIL to dc@province.gov.zw -> SENT".
// This is the audit trail the demo shows on the alert log page.
@Entity
@Table(name = "alert_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Which hazard the alert is about ("flood", "drought", ...)
    @Column(nullable = false, length = 20)
    private String hazard;

    // Incident that triggered the alert (null for manually composed test alerts)
    private Long incidentId;

    @Column(length = 50)
    private String ward;

    @Column(length = 50)
    private String district;

    @Column(length = 20)
    private String severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertChannelType channel;

    // Where it went (email address / phone number / telegram chat id)
    @Column(length = 120)
    private String recipient;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AlertStatus status;

    // Extra info: mock-mode explanation, provider error text, ...
    @Column(columnDefinition = "TEXT")
    private String detail;

    @Builder.Default
    private LocalDateTime sentAt = LocalDateTime.now();
}
