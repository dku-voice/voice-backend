package com.dku.voice.voice_backend.service;

import com.dku.voice.voice_backend.dto.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final SseEmitterService sseEmitterService;

    /**
     * order-completed 토픽 구독
     * - 주문 완료 이벤트 수신 시 해당 매장 KDS로 SSE 전송
     * - groupId: voice-backend
     * - containerFactory: KafkaConfig에서 정의한 빈 사용
     */
    @KafkaListener(
            topics = "order-completed",
            groupId = "voice-backend",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeOrderEvent(OrderEvent orderEvent) {
        log.info("[Kafka] 주문 이벤트 수신 - orderId={}, storeId={}",
                orderEvent.getOrderId(), orderEvent.getStoreId());
        sseEmitterService.sendOrderEvent(orderEvent);
    }
}