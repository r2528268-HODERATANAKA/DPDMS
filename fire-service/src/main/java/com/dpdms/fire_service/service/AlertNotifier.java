package com.dpdms.fire_service.service;

import com.dpdms.fire_service.model.FireIncident;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

// Hands every APPROVED incident to alert-service, which then sends the
// email / WhatsApp / Telegram notifications. This call is best-effort:
// if alert-service is switched off or unreachable, the approval is still saved.
@Component
public class AlertNotifier {

    private static final Logger log = LoggerFactory.getLogger(AlertNotifier.class);

    private final RestClient restClient;
    private final String alertServiceUrl;

    public AlertNotifier(@Value("${alert.service.url:}") String alertServiceUrl) {
        this.alertServiceUrl = alertServiceUrl == null ? "" : alertServiceUrl.trim();
        // 3-second timeouts: a slow or dead alert-service must never block an approval
        java.net.http.HttpClient jdk = java.net.http.HttpClient.newBuilder().build();
        org.springframework.http.client.JdkClientHttpRequestFactory factory =
                new org.springframework.http.client.JdkClientHttpRequestFactory(jdk);
        factory.setReadTimeout(java.time.Duration.ofSeconds(3));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public void notifyApproved(FireIncident incident) {
        if (alertServiceUrl.isEmpty()) {
            return; // notifications disabled — alert.service.url left empty
        }
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("hazard", "fire");
            payload.put("incidentId", incident.getId());
            payload.put("ward", incident.getWard());
            payload.put("district", incident.getDistrict());
            payload.put("severity", incident.getSeverity().name());
            payload.put("message", buildMessage(incident));
            restClient.post()
                    .uri(alertServiceUrl + "/api/alerts/send")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Handed approval alert for fire incident {} to alert-service", incident.getId());
        } catch (Exception ex) {
            log.warn("Could not reach alert-service (record is still approved): {}", ex.getMessage());
        }
    }

    private String buildMessage(FireIncident incident) {
        return String.format("[Fire - %s] APPROVED incident #%d in ward %s, %s district. %s",
                incident.getSeverity(), incident.getId(), incident.getWard(), incident.getDistrict(),
                String.format("Area burned: %.1f ha, %d structures destroyed.", incident.getAreaBurnedHectares(), incident.getStructuresDestroyed()));
    }
}
