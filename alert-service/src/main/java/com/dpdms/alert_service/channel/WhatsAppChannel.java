package com.dpdms.alert_service.channel;

import com.dpdms.alert_service.model.AlertChannelType;
import com.dpdms.alert_service.model.AlertLog;
import com.dpdms.alert_service.model.AlertStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

// WHATSAPP channel (Meta WhatsApp Cloud API).
//
// alert.whatsapp.provider=mock (default) -> MOCK mode, same log rows, nothing sent.
// alert.whatsapp.provider=meta           -> real Cloud API call:
//      POST {api-url}/{phone-number-id}/messages
//      Header: Authorization: Bearer <token>
//      Body:   {"messaging_product":"whatsapp","to":"26377...","type":"text",
//               "text":{"body":"..."}}
// See DEPLOYMENT.md for how to get a (free, test) token and phone number id.
@Slf4j
@Component
public class WhatsAppChannel implements AlertChannel {

    private final String provider;      // "mock" or "meta"
    private final String apiUrl;
    private final String phoneNumberId;
    private final String token;
    private final RestClient restClient;

    public WhatsAppChannel(@Value("${alert.whatsapp.provider:mock}") String provider,
                           @Value("${alert.whatsapp.api-url:https://graph.facebook.com/v21.0}") String apiUrl,
                           @Value("${alert.whatsapp.phone-number-id:}") String phoneNumberId,
                           @Value("${alert.whatsapp.token:}") String token) {
        this.provider = provider;
        this.apiUrl = apiUrl;
        this.phoneNumberId = phoneNumberId;
        this.token = token;
        // 5-second timeouts: a slow provider must not stall the whole alert round
        java.net.http.HttpClient jdk = java.net.http.HttpClient.newBuilder().build();
        org.springframework.http.client.JdkClientHttpRequestFactory factory =
                new org.springframework.http.client.JdkClientHttpRequestFactory(jdk);
        factory.setReadTimeout(java.time.Duration.ofSeconds(5));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public AlertChannelType type() {
        return AlertChannelType.WHATSAPP;
    }

    @Override
    public AlertLog send(AlertRequest request, String recipient) {
        if (!"meta".equalsIgnoreCase(provider)) {
            log.info("[MOCK WHATSAPP] to={} body=\"{}\"", recipient, request.getMessage());
            return AlertLog.builder()
                    .hazard(request.getHazard()).incidentId(request.getIncidentId())
                    .ward(request.getWard()).district(request.getDistrict())
                    .severity(request.getSeverity()).channel(type())
                    .recipient(recipient).message(request.getMessage())
                    .status(AlertStatus.SKIPPED)
                    .detail("MOCK mode (alert.whatsapp.provider=mock) - nothing sent")
                    .build();
        }
        try {
            Map<String, Object> payload = Map.of(
                    "messaging_product", "whatsapp",
                    "to", recipient,
                    "type", "text",
                    "text", Map.of("body", request.getMessage()));
            restClient.post()
                    .uri(apiUrl + "/" + phoneNumberId + "/messages")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            return log(request, recipient, AlertStatus.SENT, "WhatsApp message accepted by Meta");
        } catch (Exception ex) {
            log.warn("WhatsApp to {} failed: {}", recipient, ex.getMessage());
            return log(request, recipient, AlertStatus.FAILED, "Cloud API error: " + ex.getMessage());
        }
    }

    private AlertLog.AlertLogBuilder base(AlertRequest request, String recipient) {
        return AlertLog.builder()
                .hazard(request.getHazard())
                .incidentId(request.getIncidentId())
                .ward(request.getWard())
                .district(request.getDistrict())
                .severity(request.getSeverity())
                .channel(type())
                .recipient(recipient)
                .message(request.getMessage());
    }

    private AlertLog log(AlertRequest request, String recipient, AlertStatus status, String detail) {
        return base(request, recipient).status(status).detail(detail).build();
    }
}
