package com.dku.voice.voice_backend.service;

import com.dku.voice.voice_backend.dto.PaymentCancelRequest;
import com.dku.voice.voice_backend.dto.PaymentCancelResponse;
import com.dku.voice.voice_backend.dto.PaymentRequest;
import com.dku.voice.voice_backend.dto.PaymentResponse;
import com.dku.voice.voice_backend.entity.Order;
import com.dku.voice.voice_backend.entity.Payment;
import com.dku.voice.voice_backend.repository.OrderRepository;
import com.dku.voice.voice_backend.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final RedissonClient redissonClient;

    // 락 키 prefix
    private static final String PAYMENT_LOCK_PREFIX = "lock:payment:";
    // 락 대기 시간 (최대 3초 대기)
    private static final long LOCK_WAIT_TIME = 3L;
    // 락 유지 시간 (10초 후 자동 해제 - 데드락 방지)
    private static final long LOCK_LEASE_TIME = 10L;

    /**
     * 결제 승인 (4주차 - 분산 락 적용)
     * 1. Redis 분산 락 획득 (orderId 기준)
     * 2. 주문 조회
     * 3. 이미 결제된 주문인지 확인 (상태 검증)
     * 4. 위변조 검증
     * 5. pgTransactionId 중복 확인
     * 6. Payment 저장 + Order 상태 PAID 변경
     * 7. 락 해제
     */
    public PaymentResponse confirmPayment(PaymentRequest request) {
        String lockKey = PAYMENT_LOCK_PREFIX + request.getOrderId();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 락 획득 시도 (최대 3초 대기, 10초 후 자동 해제)
            boolean acquired = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);

            if (!acquired) {
                throw new IllegalStateException("현재 결제가 진행 중입니다. 잠시 후 다시 시도해주세요.");
            }

            log.info("[Payment] 분산 락 획득 - orderId={}", request.getOrderId());
            return processConfirmPayment(request);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("결제 처리 중 오류가 발생했습니다.");
        } finally {
            // 락이 현재 스레드에 의해 유지되고 있을 때만 해제
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("[Payment] 분산 락 해제 - orderId={}", request.getOrderId());
            }
        }
    }

    /**
     * 실제 결제 처리 로직 (락 안에서 실행)
     */
    @Transactional
    protected PaymentResponse processConfirmPayment(PaymentRequest request) {

        // 1. 주문 조회
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "주문을 찾을 수 없습니다. orderId=" + request.getOrderId()));

        // 2. 이미 결제된 주문인지 확인 (분산 락 안에서 상태 검증 - 최종 방어선)
        if (order.getStatus() == Order.OrderStatus.PAID) {
            throw new IllegalStateException("이미 결제 완료된 주문입니다. orderId=" + order.getId());
        }

        // 3. 위변조 검증: 클라이언트가 보낸 금액 vs 서버에서 계산한 주문 금액
        if (!order.getTotalPrice().equals(request.getAmount())) {
            throw new IllegalArgumentException(
                    "결제 금액이 주문 금액과 다릅니다. 요청=" + request.getAmount()
                    + ", 주문=" + order.getTotalPrice());
        }

        // 4. pgTransactionId 중복 확인
        if (paymentRepository.findByPgTransactionId(request.getPgTransactionId()).isPresent()) {
            throw new IllegalStateException(
                    "이미 사용된 거래 ID입니다. pgTransactionId=" + request.getPgTransactionId());
        }

        // 5. 결제 수단 파싱
        Payment.PaymentMethod method;
        try {
            method = Payment.PaymentMethod.valueOf(request.getMethod().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("지원하지 않는 결제 수단입니다: " + request.getMethod());
        }

        // 6. Payment 저장
        Payment payment = Payment.builder()
                .order(order)
                .pgProvider(request.getPgProvider().toUpperCase())
                .pgTransactionId(request.getPgTransactionId())
                .method(method)
                .amount(request.getAmount())
                .paidAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        // 7. Order 상태 PAID로 변경
        order.changeStatus(Order.OrderStatus.PAID);

        return PaymentResponse.from(payment);
    }

    /**
     * 결제 취소 / 환불 (3주차)
     * 1. pgTransactionId로 결제 조회
     * 2. Order.status로 환불 가능 여부 검증
     * 3. Order 상태 REFUNDED로 변경
     */
    @Transactional
    public PaymentCancelResponse cancelPayment(PaymentCancelRequest request) {

        // 1. pgTransactionId로 결제 조회
        Payment payment = paymentRepository.findByPgTransactionId(request.getPgTransactionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "결제 내역을 찾을 수 없습니다. pgTransactionId=" + request.getPgTransactionId()));

        // 2. Order.status로 환불 가능 여부 검증
        Order order = payment.getOrder();
        if (order.getStatus() != Order.OrderStatus.PAID) {
            throw new IllegalStateException(
                    "환불 가능한 상태가 아닙니다. 현재 주문 상태=" + order.getStatus());
        }

        // 3. Order 상태 REFUNDED로 변경
        order.changeStatus(Order.OrderStatus.REFUNDED);

        return PaymentCancelResponse.from(payment, request.getCancelReason());
    }
}