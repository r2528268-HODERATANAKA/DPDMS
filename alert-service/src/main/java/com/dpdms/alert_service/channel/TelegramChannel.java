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

// TELEGRAM channel (official Bot API - free, no approval process, easiest to demo live).
//
// 1. In Telegram, talk to @BotFather -> /newbot -> copy the bot token.
// 2. Put the token in alert.telegram.bot-token and your chat id in
//    alert.telegram.chat-ids (get a chat id from @userinfobot).
// 3. Restart: approvals now arrive as real Telegram messages.
// If the bot token is blank the channel reports SKIPPED (not configured).
@Slf4j
@Component
public class TelegramChannel implements AlertChannel {

    private final String botToken;
    private final String apiUrl;
    private final RestClient restClient;

    public TelegramChannel(@Value("${alert.telegram.bot-token:}") String botToken,
                           @Value("${alert.telegram.api-url:https://api.telegram.org}") String apiUrl) {
        this.botToken = botToken == null ? "" : botToken.trim();
        this.apiUrl = apiUrl;
        java.net.http.HttpClient jdk = java.net.http.HttpClient.newBuilder().build();
        org.springframework.http.client.JdkClientHttpRequestFactory factory =
                new org.springframework.http.client.JdkClientHttpRequestFactory(jdk);
        factory.setReadTimeout(java.time.Duration.ofSeconds(5));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public AlertChannelType type() {
        return AlertChannelType.TELEGRAM;
    }

    @Override
    public AlertLog send(AlertRequest request, String chatId) {
        if (botToken.isEmpty()) {
            log.info("[SKIPPED TELEGRAM] no bot token configured; would notify chat {}", chatId);
            return AlertLog.builder()
                    .hazard(request.getHazard()).incidentId(request.getIncidentId())
                    .ward(request.getWard()).district(request.getDistrict())
                    .severity(request.getSeverity()).channel(type())
                    .recipient(chatId).message(request.getMessage())
                    .status(AlertStatus.SKIPPED)
                    .detail("Not configured (alert.telegram.bot-token is empty)")
                    .build();
        }
        try {
            Map<String, Object> payload = Map.of(
                    "chat_id", chatId,
                    "text", request.getMessage());
            restClient.post()
                    .uri(apiUrl + "/bot" + botToken + "/sendMessage")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            return log(request, chatId, AlertStatus.SENT, "Delivered to Telegram chat " + chatId);
        } catch (Exception ex) {
            log.warn("Telegram chat {} failed: {}", chatId, ex.getMessage());
            return log(request, chatId, AlertStatus.FAILED, "Bot API error: " + ex.getMessage());
        }
    }

    private AlertLog.AlertLogBuilder base(AlertRequest request, String chatId) {
        return AlertLog.builder()
                .hazard(request.getHazard())
                .incidentId(request.getIncidentId())
                .ward(request.getWard())
                .district(request.getDistrict())
                .severity(request.getSeverity())
                .channel(type())
                .recipient(chatId)
                .message(request.getMessage());
    }

    private AlertLog log(AlertRequest request, String chatId, AlertStatus status, String detail) {
        return base(request, chatId).status(status).detail(detail).build();
    }
}
