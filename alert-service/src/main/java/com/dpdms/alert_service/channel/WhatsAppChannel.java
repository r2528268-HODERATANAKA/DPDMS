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
    private final String callmebotApiKey;   // free personal WhatsApp gateway (no business account)
    private final String gatewayUrl;        // self-hosted gateway (Evolution API) send endpoint
    private final String gatewayApiKey;
    private final RestClient restClient;

    public WhatsAppChannel(
            @Value("${alert.whatsapp.provider:mock}") String provider,
            @Value("${alert.whatsapp.api-url:https://graph.facebook.com/v21.0}") String apiUrl,
            @Value("${alert.whatsapp.phone-number-id:}") String phoneNumberId,
            @Value("${alert.whatsapp.token:}") String token,
            @Value("${alert.twilio.account-sid:}") String twilioSid,
            @Value("${alert.twilio.auth-token:}") String twilioAuthToken,
            @Value("${alert.twilio.whatsapp-from:}") String twilioFrom,
            @Value("${alert.twilio.content-sid:}") String twilioContentSid,
            @Value("${alert.callmebot.api-key:}") String callmebotApiKey,
            @Value("${alert.gateway.url:}") String gatewayUrl,
            @Value("${alert.gateway.api-key:}") String gatewayApiKey) {
        this.provider = provider;
        this.apiUrl = apiUrl;
        this.phoneNumberId = phoneNumberId;
        this.token = token;
        this.twilioSid = twilioSid;
        this.twilioAuthToken = twilioAuthToken;
        this.twilioFrom = twilioFrom;
        this.twilioContentSid = twilioContentSid == null ? "" : twilioContentSid.trim();
        this.callmebotApiKey = callmebotApiKey == null ? "" : callmebotApiKey.trim();
        this.gatewayUrl = gatewayUrl == null ? "" : gatewayUrl.trim();
        this.gatewayApiKey = gatewayApiKey == null ? "" : gatewayApiKey.trim();
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
        if ("callmebot".equalsIgnoreCase(provider)) {
            return sendViaCallMeBot(request, recipient);
        }
        if ("local_gateway".equalsIgnoreCase(provider) || "evolution".equalsIgnoreCase(provider)) {
            return sendViaLocalGateway(request, recipient);
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
        } catch (org.springframework.web.client.RestClientResponseException ex) {
            log.warn("Twilio WhatsApp to {} failed: {}", recipient, ex.getResponseBodyAsString());
            return base(request, recipient, AlertStatus.FAILED)
                    .detail("Twilio error: " + ex.getStatusCode() + " " + brief(ex.getResponseBodyAsString())).build();
        } catch (Exception ex) {
            log.warn("Twilio WhatsApp to {} failed: {}", recipient, ex.getMessage());
            return base(request, recipient, AlertStatus.FAILED)
                    .detail("Twilio error: " + ex.getMessage()).build();
        }
    }

    private AlertLog sendViaMeta(AlertRequest request, String recipient) {
        try {
            // Meta expects the bare E.164 digits (no "whatsapp:" prefix, no "+")
            Map<String, Object> payload = Map.of(
                    "messaging_product", "whatsapp",
                    "to", metaNumber(recipient),
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
        } catch (org.springframework.web.client.RestClientResponseException ex) {
            log.warn("Meta WhatsApp to {} failed: {}", recipient, ex.getResponseBodyAsString());
            return base(request, recipient, AlertStatus.FAILED)
                    .detail("Cloud API error: " + ex.getStatusCode() + " " + brief(ex.getResponseBodyAsString())).build();
        } catch (Exception ex) {
            log.warn("Meta WhatsApp to {} failed: {}", recipient, ex.getMessage());
            return base(request, recipient, AlertStatus.FAILED)
                    .detail("Cloud API error: " + ex.getMessage()).build();
        }
    }

    // CallMeBot: free personal WhatsApp gateway (recipient opts in once, gets an apikey).
    //   GET https://api.callmebot.com/whatsapp.php?phone=+263...&text=...&apikey=...
    private AlertLog sendViaCallMeBot(AlertRequest request, String recipient) {
        if (callmebotApiKey.isBlank()) {
            return base(request, recipient, AlertStatus.SKIPPED)
                    .detail("CallMeBot not configured (need alert.callmebot.api-key)").build();
        }
        try {
            String phone = metaNumber(recipient); // digits only
            String url = "https://api.callmebot.com/whatsapp.php?phone=%2B" + phone
                    + "&text=" + java.net.URLEncoder.encode(request.getMessage(), java.nio.charset.StandardCharsets.UTF_8)
                    + "&apikey=" + callmebotApiKey;
            String resp = restClient.get().uri(url).retrieve().body(String.class);
            return base(request, recipient, AlertStatus.SENT)
                    .detail("WhatsApp via CallMeBot: " + brief(resp)).build();
        } catch (org.springframework.web.client.RestClientResponseException ex) {
            return base(request, recipient, AlertStatus.FAILED)
                    .detail("CallMeBot error: " + ex.getStatusCode() + " " + brief(ex.getResponseBodyAsString())).build();
        } catch (Exception ex) {
            return base(request, recipient, AlertStatus.FAILED)
                    .detail("CallMeBot error: " + ex.getMessage()).build();
        }
    }

    // Self-hosted gateway (Evolution API). POST {gatewayUrl} with header apikey=<key>,
    // body {"number":"263...","text":"..."} where gatewayUrl ends with /message/sendText/{instance}.
    private AlertLog sendViaLocalGateway(AlertRequest request, String recipient) {
        if (gatewayUrl.isBlank()) {
            return base(request, recipient, AlertStatus.SKIPPED)
                    .detail("Local gateway not configured (need alert.gateway.url)").build();
        }
        try {
            Map<String, Object> payload = Map.of(
                    "number", metaNumber(recipient),
                    "text", request.getMessage());
            restClient.post()
                    .uri(gatewayUrl)
                    .header("apikey", gatewayApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            return base(request, recipient, AlertStatus.SENT)
                    .detail("WhatsApp delivered via self-hosted gateway").build();
        } catch (org.springframework.web.client.RestClientResponseException ex) {
            return base(request, recipient, AlertStatus.FAILED)
                    .detail("Gateway error: " + ex.getStatusCode() + " " + brief(ex.getResponseBodyAsString())).build();
        } catch (Exception ex) {
            return base(request, recipient, AlertStatus.FAILED)
                    .detail("Gateway error: " + ex.getMessage()).build();
        }
    }

    private String brief(String body) {
        if (body == null || body.isBlank()) {
            return "(no body)";
        }
        return body.length() > 300 ? body.substring(0, 300) : body;
    }

    private String metaNumber(String recipient) {
        String n = recipient == null ? "" : recipient.trim();
        if (n.startsWith("whatsapp:")) {
            n = n.substring("whatsapp:".length());
        }
        return n.replace("+", "").trim();
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
