package com.dku.voice.voice_backend.service;

import com.dku.voice.voice_backend.event.OrderPaidEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final KafkaProducerService kafkaProducerService;

    /**
     * 결제 트랜잭션이 커밋된 뒤에만 Kafka로 발행한다.
     * - 롤백되면 이 메서드는 호출되지 않아 유령 이벤트가 나가지 않는다.
     * - 이미 커밋된 결제는 Kafka 장애로 취소되면 안 되므로 예외는 삼키고 로그만 남긴다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPaid(OrderPaidEvent event) {
        try {
            kafkaProducerService.sendOrderEvent(event.orderEvent());
            log.info("[Order] 결제 완료 후 Kafka 발행 요청 - orderNumber={}",
                    event.orderEvent().getOrderNumber());
        } catch (Exception e) {
            log.error("[Order] Kafka 발행 실패 (결제는 정상 완료) - orderNumber={}, error={}",
                    event.orderEvent().getOrderNumber(), e.getMessage());
        }
    }
}