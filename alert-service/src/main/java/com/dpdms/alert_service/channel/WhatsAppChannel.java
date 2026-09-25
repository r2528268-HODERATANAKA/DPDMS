package com.dpdms.alert_service.channel;

import com.dpdms.alert_service.model.AlertChannelType;
import com.dpdms.alert_service.model.AlertLog;
import com.dpdms.alert_service.model.AlertStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

// WHATSAPP channel. Three providers:
//   provider=mock   (default) -> nothing sent, a SKIPPED log row is written.
//   provider=meta             -> Meta WhatsApp Cloud API (graph.facebook.com).
//   provider=twilio           -> Twilio Programmable Messaging (WhatsApp sandbox/sender).
//
// Twilio: POST https://api.twilio.com/2010-04-01/Accounts/{AccountSid}/Messages.json
//   Basic auth (AccountSid:AuthToken), form fields From=whatsapp:+1415... To=whatsapp:+263... Body=...
@Slf4j
@Component
public class WhatsAppChannel implements AlertChannel {

    private final String provider;          // "mock" | "meta" | "twilio"
    private final String apiUrl;            // Meta graph base
    private final String phoneNumberId;     // Meta
    private final String token;             // Meta
    private final String twilioSid;
    private final String twilioAuthToken;
    private final String twilioFrom;        // e.g. whatsapp:+14155238886
    private final String twilioContentSid;  // optional: required by Twilio for WhatsApp (Content API)
    private final RestClient restClient;

    public WhatsAppChannel(
            @Value("${alert.whatsapp.provider:mock}") String provider,
            @Value("${alert.whatsapp.api-url:https://graph.facebook.com/v21.0}") String apiUrl,
            @Value("${alert.whatsapp.phone-number-id:}") String phoneNumberId,
            @Value("${alert.whatsapp.token:}") String token,
            @Value("${alert.twilio.account-sid:}") String twilioSid,
            @Value("${alert.twilio.auth-token:}") String twilioAuthToken,
            @Value("${alert.twilio.whatsapp-from:}") String twilioFrom,
            @Value("${alert.twilio.content-sid:}") String twilioContentSid) {
        this.provider = provider;
        this.apiUrl = apiUrl;
        this.phoneNumberId = phoneNumberId;
        this.token = token;
        this.twilioSid = twilioSid;
        this.twilioAuthToken = twilioAuthToken;
        this.twilioFrom = twilioFrom;
        this.twilioContentSid = twilioContentSid == null ? "" : twilioContentSid.trim();
        java.net.http.HttpClient jdk = java.net.http.HttpClient.newBuilder().build();
        org.springframework.http.client.JdkClientHttpRequestFactory factory =
                new org.springframework.http.client.JdkClientHttpRequestFactory(jdk);
        factory.setReadTimeout(Duration.ofSeconds(8));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public AlertChannelType type() {
        return AlertChannelType.WHATSAPP;
    }

    @Override
    public AlertLog send(AlertRequest request, String recipient) {
        if ("twilio".equalsIgnoreCase(provider)) {
            return sendViaTwilio(request, recipient);
        }
        if ("meta".equalsIgnoreCase(provider)) {
            return sendViaMeta(request, recipient);
        }
        log.info("[MOCK WHATSAPP] to={} body=\"{}\"", recipient, request.getMessage());
        return base(request, recipient, AlertStatus.SKIPPED)
                .detail("MOCK mode (alert.whatsapp.provider=mock) - nothing sent")
                .build();
    }

    private AlertLog sendViaTwilio(AlertRequest request, String recipient) {
        if (twilioSid.isBlank() || twilioAuthToken.isBlank() || twilioFrom.isBlank()) {
            return base(request, recipient, AlertStatus.SKIPPED)
                    .detail("Twilio not configured (need alert.twilio.account-sid / auth-token / whatsapp-from)")
                    .build();
        }
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("From", twilioFrom);
            form.add("To", recipient.startsWith("whatsapp:") ? recipient : "whatsapp:" + recipient);
            if (!twilioContentSid.isBlank()) {
                // Twilio WhatsApp requires the Content API; send the message as template variable {{1}}.
                form.add("ContentSid", twilioContentSid);
                form.add("ContentVariables", "{\"1\":\"" + request.getMessage().replace("\\", "\\\\").replace("\"", "\\\"") + "\"}");
            } else {
                form.add("Body", request.getMessage());
            }

            String basic = Base64.getEncoder().encodeToString(
                    (twilioSid + ":" + twilioAuthToken).getBytes(StandardCharsets.UTF_8));

            restClient.post()
                    .uri("https://api.twilio.com/2010-04-01/Accounts/" + twilioSid + "/Messages.json")
                    .header("Authorization", "Basic " + basic)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
            return base(request, recipient, AlertStatus.SENT)
                    .detail("WhatsApp delivered via Twilio").build();
        } catch (Exception ex) {
            log.warn("Twilio WhatsApp to {} failed: {}", recipient, ex.getMessage());
            return base(request, recipient, AlertStatus.FAILED)
                    .detail("Twilio error: " + ex.getMessage()).build();
        }
    }

    private AlertLog sendViaMeta(AlertRequest request, String recipient) {
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
            return base(request, recipient, AlertStatus.SENT)
                    .detail("WhatsApp message accepted by Meta").build();
        } catch (Exception ex) {
            log.warn("Meta WhatsApp to {} failed: {}", recipient, ex.getMessage());
            return base(request, recipient, AlertStatus.FAILED)
                    .detail("Cloud API error: " + ex.getMessage()).build();
        }
    }

    private AlertLog.AlertLogBuilder base(AlertRequest request, String recipient, AlertStatus status) {
        return AlertLog.builder()
                .hazard(request.getHazard())
                .incidentId(request.getIncidentId())
                .ward(request.getWard())
                .district(request.getDistrict())
                .severity(request.getSeverity())
                .channel(type())
                .recipient(recipient)
                .message(request.getMessage())
                .status(status);
    }
}
