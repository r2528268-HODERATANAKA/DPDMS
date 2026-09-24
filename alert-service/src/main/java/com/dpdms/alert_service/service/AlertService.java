package com.dpdms.alert_service.service;

import com.dpdms.alert_service.channel.AlertChannel;
import com.dpdms.alert_service.channel.AlertRequest;
import com.dpdms.alert_service.model.AlertChannelType;
import com.dpdms.alert_service.model.AlertLog;
import com.dpdms.alert_service.model.AlertStatus;
import com.dpdms.alert_service.repository.AlertLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// The brain of alert-service.
//
//  sendAlert(request)  - fan one alert out through ALL channels (used by the
//                        controller for manual demo alerts AND by the hazard
//                        services, which POST here on every approval).
//  scanApproved()      - pull approved incidents from all five hazard services
//                        and alert the ones we have not alerted yet.
//
// Every attempt on every channel is written to alert_logs (SENT / FAILED / SKIPPED).
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertLogRepository repository;
    private final List<AlertChannel> channels;      // Spring injects EmailChannel, WhatsAppChannel, TelegramChannel
    private final ApprovedIncidentFetcher fetcher;

    @Value("${alert.recipients.email:}")
    private String emailRecipients;                 // comma-separated list

    @Value("${alert.recipients.whatsapp:}")
    private String whatsappRecipients;

    @Value("${alert.recipients.telegram:}")
    private String telegramChatIds;

    private static final List<String> HAZARDS =
            List.of("flood", "drought", "fire", "zoonotic", "mining");

    // ---------- One alert, all channels ----------

    public List<AlertLog> sendAlert(AlertRequest request) {
        List<AlertLog> results = new ArrayList<>();
        for (AlertChannel channel : channels) {
            List<String> recipients = recipientsFor(channel.type());
            if (recipients.isEmpty()) {
                // Nothing configured for this channel - still log it so the demo
                // shows WHY nothing was sent on that channel.
                results.add(AlertLog.builder()
                        .hazard(request.getHazard())
                        .incidentId(request.getIncidentId())
                        .ward(request.getWard())
                        .district(request.getDistrict())
                        .severity(request.getSeverity())
                        .channel(channel.type())
                        .message(request.getMessage())
                        .status(AlertStatus.SKIPPED)
                        .detail("No recipients configured for " + channel.type())
                        .build());
                continue;
            }
            for (String recipient : recipients) {
                results.add(channel.send(request, recipient));
            }
        }
        List<AlertLog> saved = repository.saveAll(results);
        log.info("Alert for {} incident {}: {} channel results",
                request.getHazard(), request.getIncidentId(), saved.size());
        return saved;
    }

    // ---------- Scan hazard services for newly approved incidents ----------

    public Map<String, Object> scanApproved() {
        int alerted = 0;
        int skipped = 0;

        for (String hazard : HAZARDS) {
            List<Map<String, Object>> approved = fetcher.fetchApproved(hazard);
            for (Map<String, Object> incident : approved) {
                Long incidentId = asLong(incident.get("id"));
                if (incidentId == null) {
                    continue;
                }
                if (repository.existsByHazardAndIncidentId(hazard, incidentId)) {
                    skipped++; // already alerted in a previous scan
                    continue;
                }
                AlertRequest request = AlertRequest.builder()
                        .hazard(hazard)
                        .incidentId(incidentId)
                        .ward(asString(incident.get("ward")))
                        .district(asString(incident.get("district")))
                        .severity(asString(incident.get("severity")))
                        .message("APPROVED " + hazard.toUpperCase()
                                + " incident #" + incidentId
                                + " in ward " + asString(incident.get("ward"))
                                + ", " + asString(incident.get("district"))
                                + " district (severity: " + asString(incident.get("severity")) + ")")
                        .build();
                sendAlert(request);
                alerted++;
            }
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("scannedHazards", HAZARDS.size());
        summary.put("alertedIncidents", alerted);
        summary.put("alreadyAlertedSkipped", skipped);
        return summary;
    }

    // ---------- Read access for the alert log page ----------

    public List<AlertLog> allLogs() {
        return repository.findAllByOrderBySentAtDesc();
    }

    public List<AlertLog> logsForHazard(String hazard) {
        return repository.findByHazardOrderBySentAtDesc(hazard.toLowerCase());
    }

    // ---------- Helpers ----------

    private List<String> recipientsFor(AlertChannelType type) {
        return switch (type) {
            case EMAIL -> split(emailRecipients);
            case WHATSAPP -> split(whatsappRecipients);
            case TELEGRAM -> split(telegramChatIds);
        };
    }

    private List<String> split(String commaSeparated) {
        if (commaSeparated == null || commaSeparated.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(commaSeparated.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private Long asLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String asString(Object value) {
        return value == null ? "" : value.toString();
    }
}
