package com.dpdms.alert_service.channel;

import com.dpdms.alert_service.model.AlertChannelType;
import com.dpdms.alert_service.model.AlertLog;
import com.dpdms.alert_service.model.AlertStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

// EMAIL channel. Two providers:
//   provider=smtp     (default) -> Spring JavaMailSender (Gmail app password etc.)
//   provider=sendgrid           -> Twilio SendGrid v3 Mail Send API (Bearer API key)
//
// email.enabled=false (default) -> MOCK mode: nothing leaves the machine, same log row.
@Slf4j
@Component
public class EmailChannel implements AlertChannel {

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String fromAddress;
    private final String provider;        // "smtp" | "sendgrid"
    private final String sendgridApiKey;
    private final RestClient restClient;

    public EmailChannel(JavaMailSender mailSender,
                        @Value("${alert.email.enabled:false}") boolean enabled,
                        @Value("${alert.email.from:dpdms-alerts@example.com}") String fromAddress,
                        @Value("${alert.email.provider:smtp}") String provider,
                        @Value("${alert.sendgrid.api-key:}") String sendgridApiKey) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.fromAddress = fromAddress;
        this.provider = provider;
        this.sendgridApiKey = sendgridApiKey;
        java.net.http.HttpClient jdk = java.net.http.HttpClient.newBuilder().build();
        org.springframework.http.client.JdkClientHttpRequestFactory factory =
                new org.springframework.http.client.JdkClientHttpRequestFactory(jdk);
        factory.setReadTimeout(Duration.ofSeconds(8));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public AlertChannelType type() {
        return AlertChannelType.EMAIL;
    }

    @Override
    public AlertLog send(AlertRequest request, String recipient) {
        if (!enabled) {
            return mockLog(request, recipient,
                    "MOCK mode (alert.email.enabled=false) - message printed to console only");
        }
        return "sendgrid".equalsIgnoreCase(provider)
                ? sendViaSendGrid(request, recipient)
                : sendViaSmtp(request, recipient);
    }

    private AlertLog sendViaSmtp(AlertRequest request, String recipient) {
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(fromAddress);
            mail.setTo(recipient);
            mail.setSubject("DPDMS alert: " + request.getSeverity() + " " + request.getHazard());
            mail.setText(request.getMessage());
            mailSender.send(mail);
            return base(request, recipient, AlertStatus.SENT).detail("Email delivered via SMTP").build();
        } catch (Exception ex) {
            log.warn("Email to {} failed: {}", recipient, ex.getMessage());
            return base(request, recipient, AlertStatus.FAILED).detail("SMTP error: " + ex.getMessage()).build();
        }
    }

    // Twilio SendGrid v3 Mail Send API.
    private AlertLog sendViaSendGrid(AlertRequest request, String recipient) {
        if (sendgridApiKey == null || sendgridApiKey.isBlank()) {
            return base(request, recipient, AlertStatus.SKIPPED)
                    .detail("SendGrid not configured (need alert.sendgrid.api-key)").build();
        }
        try {
            Map<String, Object> payload = Map.of(
                    "personalizations", List.of(Map.of("to", List.of(Map.of("email", recipient)))),
                    "from", Map.of("email", fromAddress),
                    "subject", "DPDMS alert: " + request.getSeverity() + " " + request.getHazard(),
                    "content", List.of(Map.of("type", "text/plain", "value", request.getMessage())));
            org.springframework.http.ResponseEntity<Void> resp = restClient.post()
                    .uri("https://api.sendgrid.com/v3/mail/send")
                    .header("Authorization", "Bearer " + sendgridApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            String msgId = resp.getHeaders().getFirst("X-Message-Id");
            return base(request, recipient, AlertStatus.SENT)
                    .detail("SendGrid accepted (HTTP " + resp.getStatusCode().value()
                            + (msgId != null ? ", X-Message-Id=" + msgId : "") + ")").build();
        } catch (Exception ex) {
            log.warn("SendGrid email to {} failed: {}", recipient, ex.getMessage());
            return base(request, recipient, AlertStatus.FAILED)
                    .detail("SendGrid error: " + ex.getMessage()).build();
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

    private AlertLog mockLog(AlertRequest request, String recipient, String detail) {
        log.info("[MOCK EMAIL] to={} subject=\"DPDMS alert: {} {}\" body=\"{}\"",
                recipient, request.getSeverity(), request.getHazard(), request.getMessage());
        return base(request, recipient, AlertStatus.SKIPPED).detail(detail).build();
    }
}
