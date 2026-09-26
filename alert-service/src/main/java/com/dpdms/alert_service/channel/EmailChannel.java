package com.dpdms.alert_service.channel;

import com.dpdms.alert_service.model.AlertChannelType;
import com.dpdms.alert_service.model.AlertLog;
import com.dpdms.alert_service.model.AlertStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

// EMAIL channel.
//
// alert.email.enabled=false (default) -> MOCK mode: nothing leaves the machine,
// but the exact same log row is produced, so the demo works anywhere.
// alert.email.enabled=true            -> real SMTP via spring-boot-starter-mail
//                                        (Gmail app password: see DEPLOYMENT.md).
@Slf4j
@Component
public class EmailChannel implements AlertChannel {

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String fromAddress;

    public EmailChannel(JavaMailSender mailSender,
                        @Value("${alert.email.enabled:false}") boolean enabled,
                        @Value("${alert.email.from:dpdms-alerts@example.com}") String fromAddress) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.fromAddress = fromAddress;
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
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(fromAddress);
            mail.setTo(recipient);
            mail.setSubject("DPDMS alert: " + request.getSeverity() + " " + request.getHazard());
            mail.setText(request.getMessage());
            mailSender.send(mail);
            return base(request, recipient, AlertStatus.SENT)
                    .detail("Email delivered via SMTP").build();
        } catch (Exception ex) {
            log.warn("Email to {} failed: {}", recipient, ex.getMessage());
            return base(request, recipient, AlertStatus.FAILED)
                    .detail("SMTP error: " + ex.getMessage()).build();
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
