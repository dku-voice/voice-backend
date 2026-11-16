package com.dku.voice.voice_backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * 결제 이상 알림. ALERT_DISCORD_WEBHOOK_URL(alert.discord-webhook-url)이 비어 있으면 로그만 남긴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentAlertNotifier {

    private final RestClient restClient;

    @Value("${alert.discord-webhook-url:}")
    private String webhookUrl;

    public void send(String message) {
        log.error("[ALERT] {}", message);
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }
        try {
            String content = message.length() > 1900 ? message.substring(0, 1900) : message;
            restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("content", content))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("[ALERT] 알림 전송 실패: {}", e.getMessage());
        }
    }
}