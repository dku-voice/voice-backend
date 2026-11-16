package com.dku.voice.voice_backend.service;

import com.dku.voice.voice_backend.dto.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class SseEmitterService {

    /**
     * storeId → List<SseEmitter>
     * - 같은 매장에 여러 KDS 화면이 연결될 수 있으므로 List 사용
     * - ConcurrentHashMap: 여러 스레드(Kafka Consumer)가 동시 접근 → Thread-safe
     * - CopyOnWriteArrayList: Emitter 전송 중 삭제 발생해도 안전
     */
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /**
     * KDS 화면 연결 시 SseEmitter 생성 및 등록
     * - timeout 0L: 무제한 (매장 운영 중 계속 연결 유지)
     * - 연결 종료/타임아웃/에러 시 자동 제거
     */
    public SseEmitter connect(String storeId) {
        SseEmitter emitter = new SseEmitter(0L);

        emitters.computeIfAbsent(storeId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        log.info("[SSE] KDS 연결 - storeId={}, 현재 연결 수={}",
                storeId, emitters.get(storeId).size());

        emitter.onCompletion(() -> remove(storeId, emitter));
        emitter.onTimeout(() -> remove(storeId, emitter));
        emitter.onError(e -> remove(storeId, emitter));

        // 연결 직후 더미 이벤트 전송 (연결 확인용)
        try {
            emitter.send(SseEmitter.event().name("connect").data("KDS 연결 완료"));
        } catch (IOException e) {
            log.warn("[SSE] 초기 연결 이벤트 전송 실패 - storeId={}", storeId);
            remove(storeId, emitter);
        }

        return emitter;
    }

    /**
     * 주문 이벤트를 해당 매장 KDS로 전송
     * - KafkaConsumerService에서 호출
     * - 전송 실패한 Emitter는 즉시 제거
     */
    public void sendOrderEvent(OrderEvent orderEvent) {
        String storeId = orderEvent.getStoreId();
        List<SseEmitter> storeEmitters = emitters.get(storeId);

        if (storeEmitters == null || storeEmitters.isEmpty()) {
            log.warn("[SSE] 연결된 KDS 없음 - storeId={}", storeId);
            return;
        }

        storeEmitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("new-order")
                        .data(orderEvent));
                log.info("[SSE] 주문 이벤트 전송 - storeId={}, orderId={}",
                        storeId, orderEvent.getOrderId());
            } catch (IOException e) {
                log.warn("[SSE] 이벤트 전송 실패 - storeId={}, error={}",
                        storeId, e.getMessage());
                remove(storeId, emitter);
            }
        });
    }

    /**
     * Emitter 제거
     */
    private void remove(String storeId, SseEmitter emitter) {
        List<SseEmitter> storeEmitters = emitters.get(storeId);
        if (storeEmitters != null) {
            storeEmitters.remove(emitter);
            log.info("[SSE] KDS 연결 해제 - storeId={}, 남은 연결 수={}",
                    storeId, storeEmitters.size());
        }
    }
}