package com.dku.voice.voice_backend.service;

import com.dku.voice.voice_backend.event.MenuStatusChangedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class AiMenuSyncListener {

    private final RestClient restClient;
    private final String aiServerUrl;

    // 기본값이 있어서 application.yml을 수정하지 않아도 된다.
    // 배포에서는 환경변수 AI_SERVER_URL로 덮어쓴다.
    public AiMenuSyncListener(@Value("${ai.server.url:http://localhost:8000}") String aiServerUrl) {
        // 기본 RestClient는 타임아웃이 없어서 AI 서버가 멈추면 관리자 요청이 같이 멈춘다.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        this.restClient = RestClient.builder().requestFactory(factory).build();
        this.aiServerUrl = aiServerUrl;
    }

    /**
     * 메뉴 상태 변경이 DB에 커밋된 뒤에만 AI 서버에 재동기화를 요청한다.
     * - AI 서버가 이 알림을 받으면 GET /api/ai/menus를 다시 호출해 메뉴 데이터를 갱신한다.
     * - AI 서버가 꺼져 있거나 느려도 메뉴 상태 변경은 이미 커밋됐으므로 예외는 삼키고 로그만 남긴다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMenuStatusChanged(MenuStatusChangedEvent event) {
        try {
            restClient.post()
                    .uri(aiServerUrl + "/api/menus/reload")
                    .retrieve()
                    .toBodilessEntity();
            log.info("[AI] 메뉴 재동기화 요청 완료 - menuId={}", event.getMenuId());
        } catch (Exception e) {
            log.warn("[AI] 메뉴 재동기화 요청 실패 (메뉴 변경은 정상 반영됨) - menuId={}, error={}",
                    event.getMenuId(), e.getMessage());
        }
    }
}