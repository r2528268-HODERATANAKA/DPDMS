package com.dpdms.alert_service;

import com.dpdms.alert_service.channel.AlertChannel;
import com.dpdms.alert_service.channel.AlertRequest;
import com.dpdms.alert_service.model.AlertChannelType;
import com.dpdms.alert_service.model.AlertLog;
import com.dpdms.alert_service.model.AlertStatus;
import com.dpdms.alert_service.repository.AlertLogRepository;
import com.dpdms.alert_service.service.AlertService;
import com.dpdms.alert_service.service.ApprovedIncidentFetcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

// Unit tests for the alert fan-out and scan logic. Channels and repository are mocked.
@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertLogRepository repository;

    @Mock
    private AlertChannel emailChannel;

    @Mock
    private AlertChannel whatsappChannel;

    @Mock
    private AlertChannel telegramChannel;

    @Mock
    private ApprovedIncidentFetcher fetcher;

    private AlertService service; // built manually so the channel list is explicit

    private AlertRequest request;

    @BeforeEach
    void setUp() {
        service = new AlertService(repository,
                List.of(emailChannel, whatsappChannel, telegramChannel), fetcher);
        request = AlertRequest.builder()
                .hazard("flood").incidentId(12L)
                .ward("Mudzi").district("Mudzi").severity("HIGH")
                .message("APPROVED flood incident #12")
                .build();
    }

    private AlertLog logRow(AlertChannelType channel, AlertStatus status) {
        return AlertLog.builder()
                .hazard("flood").incidentId(12L).channel(channel)
                .status(status).message("x").build();
    }

    @Test
    void sendAlertFansOutToAllConfiguredChannelsAndSaves() {
        ReflectionTestUtils.setField(service, "emailRecipients", "dc@province.gov.zw");
        ReflectionTestUtils.setField(service, "whatsappRecipients", "263771234567");
        ReflectionTestUtils.setField(service, "telegramChatIds", "");

        when(emailChannel.type()).thenReturn(AlertChannelType.EMAIL);
        when(emailChannel.send(any(), eq("dc@province.gov.zw")))
                .thenReturn(logRow(AlertChannelType.EMAIL, AlertStatus.SENT));
        when(whatsappChannel.type()).thenReturn(AlertChannelType.WHATSAPP);
        when(whatsappChannel.send(any(), eq("263771234567")))
                .thenReturn(logRow(AlertChannelType.WHATSAPP, AlertStatus.SENT));
        when(telegramChannel.type()).thenReturn(AlertChannelType.TELEGRAM);
        when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<AlertLog> results = service.sendAlert(request);

        // email + whatsapp sent, telegram has no recipients -> 3 log rows in total
        assertEquals(3, results.size());
        assertEquals(2, results.stream().filter(r -> r.getStatus() == AlertStatus.SENT).count());
        verify(repository).saveAll(anyList());
    }

    @Test
    void channelWithoutRecipientsIsLoggedAsSkippedNotForgotten() {
        ReflectionTestUtils.setField(service, "emailRecipients", "dc@province.gov.zw");
        ReflectionTestUtils.setField(service, "whatsappRecipients", "");
        ReflectionTestUtils.setField(service, "telegramChatIds", "");

        when(emailChannel.type()).thenReturn(AlertChannelType.EMAIL);
        when(emailChannel.send(any(), eq("dc@province.gov.zw")))
                .thenReturn(logRow(AlertChannelType.EMAIL, AlertStatus.SENT));
        when(whatsappChannel.type()).thenReturn(AlertChannelType.WHATSAPP);
        when(telegramChannel.type()).thenReturn(AlertChannelType.TELEGRAM);
        when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<AlertLog> results = service.sendAlert(request);

        long skipped = results.stream().filter(r -> r.getStatus() == AlertStatus.SKIPPED).count();
        assertEquals(2, skipped); // whatsapp + telegram have no recipients
        // and the skipped rows still say why
        assertTrue(results.stream().anyMatch(r ->
                r.getDetail() != null && r.getDetail().contains("No recipients")));
    }

    @Test
    void failedChannelNeverStopsTheOtherChannels() {
        ReflectionTestUtils.setField(service, "emailRecipients", "dc@province.gov.zw");
        ReflectionTestUtils.setField(service, "whatsappRecipients", "263771234567");
        ReflectionTestUtils.setField(service, "telegramChatIds", "555000111");

        when(emailChannel.type()).thenReturn(AlertChannelType.EMAIL);
        when(emailChannel.send(any(), eq("dc@province.gov.zw")))
                .thenReturn(logRow(AlertChannelType.EMAIL, AlertStatus.FAILED));
        when(whatsappChannel.type()).thenReturn(AlertChannelType.WHATSAPP);
        when(whatsappChannel.send(any(), eq("263771234567")))
                .thenReturn(logRow(AlertChannelType.WHATSAPP, AlertStatus.SENT));
        when(telegramChannel.type()).thenReturn(AlertChannelType.TELEGRAM);
        when(telegramChannel.send(any(), eq("555000111")))
                .thenReturn(logRow(AlertChannelType.TELEGRAM, AlertStatus.SENT));
        when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<AlertLog> results = service.sendAlert(request);

        assertEquals(3, results.size());
        assertTrue(results.stream().anyMatch(r -> r.getStatus() == AlertStatus.FAILED));
        assertEquals(2, results.stream().filter(r -> r.getStatus() == AlertStatus.SENT).count());
    }

    @Test
    void scanAlertsNewlyApprovedIncidentsOnce() {
        when(fetcher.fetchApproved("flood")).thenReturn(List.of(
                Map.of("id", 12, "ward", "Mudzi", "district", "Mudzi", "severity", "HIGH")));
        when(fetcher.fetchApproved("drought")).thenReturn(List.of());
        when(fetcher.fetchApproved("fire")).thenReturn(List.of());
        when(fetcher.fetchApproved("zoonotic")).thenReturn(List.of());
        when(fetcher.fetchApproved("mining")).thenReturn(List.of());

        // this incident has never been alerted
        when(repository.existsByHazardAndIncidentId("flood", 12L)).thenReturn(false);

        // only email has recipients configured for this scan
        ReflectionTestUtils.setField(service, "emailRecipients", "dc@province.gov.zw");
        when(emailChannel.type()).thenReturn(AlertChannelType.EMAIL);
        when(emailChannel.send(any(), anyString()))
                .thenReturn(logRow(AlertChannelType.EMAIL, AlertStatus.SENT));
        when(whatsappChannel.type()).thenReturn(AlertChannelType.WHATSAPP);
        when(telegramChannel.type()).thenReturn(AlertChannelType.TELEGRAM);
        when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> summary = service.scanApproved();

        assertEquals(1, summary.get("alertedIncidents"));
        assertEquals(0, summary.get("alreadyAlertedSkipped"));
        verify(emailChannel).send(any(), eq("dc@province.gov.zw"));
    }

    @Test
    void scanSkipsIncidentsAlreadyAlerted() {
        when(fetcher.fetchApproved("flood")).thenReturn(List.of(
                Map.of("id", 12, "ward", "Mudzi", "district", "Mudzi", "severity", "HIGH")));
        when(fetcher.fetchApproved("drought")).thenReturn(List.of());
        when(fetcher.fetchApproved("fire")).thenReturn(List.of());
        when(fetcher.fetchApproved("zoonotic")).thenReturn(List.of());
        when(fetcher.fetchApproved("mining")).thenReturn(List.of());

        when(repository.existsByHazardAndIncidentId("flood", 12L)).thenReturn(true);

        Map<String, Object> summary = service.scanApproved();

        assertEquals(0, summary.get("alertedIncidents"));
        assertEquals(1, summary.get("alreadyAlertedSkipped"));
        verifyNoInteractions(emailChannel);
    }

    @Test
    void scanWithEmptyFeedsDoesNothing() {
        when(fetcher.fetchApproved(anyString())).thenReturn(List.of());

        Map<String, Object> summary = service.scanApproved();

        assertEquals(0, summary.get("alertedIncidents"));
        verify(repository, never()).saveAll(anyList());
    }
}
