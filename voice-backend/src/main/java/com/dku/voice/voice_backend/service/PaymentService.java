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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final RedissonClient redissonClient;
    private final RestClient restClient;

    @Lazy
    @Autowired
    private PaymentService self;

    @Value("${toss.secret-key}")
    private String tossSecretKey;

    private static final String PAYMENT_LOCK_PREFIX = "lock:payment:";
    private static final long LOCK_WAIT_TIME = 3L;
    private static final long LOCK_LEASE_TIME = 10L;

    // 토스 API URL
    private static final String TOSS_CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";
    private static final String TOSS_CANCEL_URL  = "https://api.tosspayments.com/v1/payments/{paymentKey}/cancel";


    public void setSelf(PaymentService self) { this.self = self;}
    /**
     * 결제 승인
     * 1. Redis 분산 락 획득
     * 2. 주문 조회 및 상태/금액 검증
     * 3. 토스 승인 API 호출 (pgProvider=TOSS일 때)
     * 4. Payment 저장 + Order 상태 PAID
     * 5. 락 해제
     */
    public PaymentResponse confirmPayment(PaymentRequest request) {
        String lockKey = PAYMENT_LOCK_PREFIX + request.getOrderId();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if (!acquired) {
                throw new IllegalStateException("현재 결제가 진행 중입니다. 잠시 후 다시 시도해주세요.");
            }
            log.info("[Payment] 분산 락 획득 - orderId={}", request.getOrderId());
            return self.processConfirmPayment(request);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("결제 처리 중 오류가 발생했습니다.");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("[Payment] 분산 락 해제 - orderId={}", request.getOrderId());
            }
        }
    }

    @Transactional
    public PaymentResponse processConfirmPayment(PaymentRequest request) {

        // 1. 주문 조회
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "주문을 찾을 수 없습니다. orderId=" + request.getOrderId()));

        // 2. 이미 결제된 주문 확인
        if (order.getStatus() == Order.OrderStatus.PAID) {
            throw new IllegalStateException("이미 결제 완료된 주문입니다. orderId=" + order.getId());
        }

        // 3. 위변조 검증
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

        // 6. 토스 승인 API 호출
        if ("TOSS".equalsIgnoreCase(request.getPgProvider())) {
            callTossConfirmApi(request.getPgTransactionId(), request.getOrderId(), request.getAmount());
        }

        // 7. Payment 저장
        Payment payment = Payment.builder()
                .order(order)
                .pgProvider(request.getPgProvider().toUpperCase())
                .pgTransactionId(request.getPgTransactionId())
                .method(method)
                .amount(request.getAmount())
                .paidAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        // 8. Order 상태 PAID
        order.changeStatus(Order.OrderStatus.PAID);

        return PaymentResponse.from(payment);
    }

    /**
     * 결제 취소 / 환불
     */
    @Transactional
    public PaymentCancelResponse cancelPayment(PaymentCancelRequest request) {

        Payment payment = paymentRepository.findByPgTransactionId(request.getPgTransactionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "결제 내역을 찾을 수 없습니다. pgTransactionId=" + request.getPgTransactionId()));

        Order order = payment.getOrder();
        if (order.getStatus() != Order.OrderStatus.PAID) {
            throw new IllegalStateException(
                    "환불 가능한 상태가 아닙니다. 현재 주문 상태=" + order.getStatus());
        }

        // 토스 취소 API 호출
        if ("TOSS".equalsIgnoreCase(payment.getPgProvider())) {
            callTossCancelApi(payment.getPgTransactionId(), request.getCancelReason());
        }

        order.changeStatus(Order.OrderStatus.REFUNDED);

        return PaymentCancelResponse.from(payment, request.getCancelReason());
    }

    // ── 토스 API 호출 ─────────────────────────────────────────────────────────

    /**
     * 토스 결제 승인 API 호출
     * POST https://api.tosspayments.com/v1/payments/confirm
     */
    private void callTossConfirmApi(String paymentKey, Long orderId, Integer amount) {
        String authorization = "Basic " + Base64.getEncoder()
                .encodeToString((tossSecretKey + ":").getBytes(StandardCharsets.UTF_8));

        try {
            restClient.post()
                    .uri(TOSS_CONFIRM_URL)
                    .header("Authorization", authorization)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "paymentKey", paymentKey,
                            "orderId",    orderId.toString(),
                            "amount",     amount
                    ))
                    .retrieve()
                    .toBodilessEntity();

            log.info("[Toss] 결제 승인 성공 - paymentKey={}", paymentKey);

        } catch (Exception e) {
            log.error("[Toss] 결제 승인 실패 - paymentKey={}, error={}", paymentKey, e.getMessage());
            throw new IllegalStateException("토스 결제 승인 중 오류가 발생했습니다.");
        }
    }

    /**
     * 토스 결제 취소 API 호출
     * POST https://api.tosspayments.com/v1/payments/{paymentKey}/cancel
     */
    private void callTossCancelApi(String paymentKey, String cancelReason) {
        String authorization = "Basic " + Base64.getEncoder()
                .encodeToString((tossSecretKey + ":").getBytes(StandardCharsets.UTF_8));

        try {
            restClient.post()
                    .uri(TOSS_CANCEL_URL, paymentKey)
                    .header("Authorization", authorization)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("cancelReason", cancelReason))
                    .retrieve()
                    .toBodilessEntity();

            log.info("[Toss] 결제 취소 성공 - paymentKey={}", paymentKey);

        } catch (Exception e) {
            log.error("[Toss] 결제 취소 실패 - paymentKey={}, error={}", paymentKey, e.getMessage());
            throw new IllegalStateException("토스 결제 취소 중 오류가 발생했습니다.");
        }
    }
}