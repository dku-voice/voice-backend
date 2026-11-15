package com.dku.voice.voice_backend.service;

import com.dku.voice.voice_backend.entity.Order;
import com.dku.voice.voice_backend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 결제 시도 후 결과를 확인하지 못한 PENDING 주문을 주기적으로 토스에 재조회한다.
 * 뒤늦게 승인된 결제는 PaymentService.reconcile 이 자동 환불한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentReconciliationScheduler {

    private final OrderRepository orderRepository;
    private final PaymentService paymentService;

    /** 마지막 시도/확인 후 이 시간(분)이 지나야 재확인 대상 */
    @Value("${payment.reconcile.delay-minutes:15}")
    private long delayMinutes;

    @Scheduled(initialDelayString = "${payment.reconcile.initial-delay-ms:60000}",
               fixedDelayString = "${payment.reconcile.interval-ms:300000}")
    public void run() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(delayMinutes);
        List<Order> targets = orderRepository.findPaymentCheckTargets(
                Order.OrderStatus.PENDING, PaymentService.RECONCILE_MAX_CHECKS, threshold);
        if (targets.isEmpty()) {
            return;
        }
        log.info("[Reconcile] 재확인 대상 {}건", targets.size());
        for (Order order : targets) {
            try {
                paymentService.reconcile(order.getId());
            } catch (Exception e) {
                log.error("[Reconcile] 처리 실패 - orderId={}, error={}", order.getId(), e.toString());
            }
        }
    }
}