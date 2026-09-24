package com.dpdms.alert_service;

import com.dpdms.alert_service.channel.AlertRequest;
import com.dpdms.alert_service.channel.EmailChannel;
import com.dpdms.alert_service.model.AlertChannelType;
import com.dpdms.alert_service.model.AlertLog;
import com.dpdms.alert_service.model.AlertStatus;
import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Unit tests for the email channel's three modes: mock, real send, provider failure.
class EmailChannelTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final AlertRequest request = AlertRequest.builder()
            .hazard("flood").incidentId(12L).ward("Mudzi")
            .district("Mudzi").severity("HIGH").message("Test alert")
            .build();

    @Test
    void mockModeProducesSkippedLogAndSendsNothing() {
        EmailChannel channel = new EmailChannel(mailSender, false, "dpdms@example.com", "smtp", "");

        AlertLog result = channel.send(request, "dc@province.gov.zw");

        assertEquals(AlertStatus.SKIPPED, result.getStatus());
        assertTrue(result.getDetail().contains("MOCK"));
        verifyNoInteractions(mailSender);
        assertEquals(AlertChannelType.EMAIL, result.getChannel());
        assertEquals("dc@province.gov.zw", result.getRecipient());
    }

    @Test
    void enabledModeSendsEmailAndLogsSent() {
        EmailChannel channel = new EmailChannel(mailSender, true, "dpdms@example.com", "smtp", "");

        AlertLog result = channel.send(request, "dc@province.gov.zw");

        assertEquals(AlertStatus.SENT, result.getStatus());
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void smtpFailureIsLoggedAsFailedNotThrown() {
        doThrow(new RuntimeException("connection refused"))
                .when(mailSender).send(any(SimpleMailMessage.class));
        EmailChannel channel = new EmailChannel(mailSender, true, "dpdms@example.com", "smtp", "");

        AlertLog result = channel.send(request, "dc@province.gov.zw");

        assertEquals(AlertStatus.FAILED, result.getStatus());
        assertTrue(result.getDetail().contains("connection refused"));
    }
}
