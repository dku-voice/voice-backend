package com.dku.voice.voice_backend.service;

import com.dku.voice.voice_backend.dto.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    private static final String ORDER_TOPIC = "order-completed";

    /**
     * 주문 완료 이벤트 발행
     * - key: orderNumber (같은 주문은 같은 파티션으로)
     * - value: OrderEvent (주문 정보)
     */
    public void sendOrderEvent(OrderEvent orderEvent) {
        kafkaTemplate.send(ORDER_TOPIC, orderEvent.getOrderNumber(), orderEvent)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka] 주문 이벤트 발행 실패 - orderNumber={}, error={}",
                                orderEvent.getOrderNumber(), ex.getMessage());
                    } else {
                        log.info("[Kafka] 주문 이벤트 발행 성공 - orderNumber={}, partition={}",
                                orderEvent.getOrderNumber(),
                                result.getRecordMetadata().partition());
                    }
                });
    }
}