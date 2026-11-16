package com.dku.voice.voice_backend.service;

import com.dku.voice.voice_backend.dto.OrderEvent;
import com.dku.voice.voice_backend.dto.PaymentCancelRequest;
import com.dku.voice.voice_backend.dto.PaymentCancelResponse;
import com.dku.voice.voice_backend.dto.PaymentRequest;
import com.dku.voice.voice_backend.dto.PaymentResponse;
import com.dku.voice.voice_backend.entity.Order;
import com.dku.voice.voice_backend.entity.Payment;
import com.dku.voice.voice_backend.event.OrderPaidEvent;
import com.dku.voice.voice_backend.repository.OrderRepository;
import com.dku.voice.voice_backend.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 결제 승인/취소 + 토스 연동.
 *
 * 결제 승인 흐름 (confirmPayment)
 *  1. [TX] preparePayment   : 주문/금액/중복/결제수단 검증 + "결제 시도 시각" 기록(커밋)
 *  2. 토스 승인 호출         : 응답 없음/409/5xx 는 같은 Idempotency-Key 로 재시도
 *  3. 그래도 모르면 토스 조회  : DONE=승인됨, ABORTED/EXPIRED/CANCELED=실패, 그 외=불명(보류)
 *  4. [TX] finalizePayment  : Payment 저장 + PAID (+ 커밋 후 Kafka 이벤트)
 *     - 4번이 커밋 실패까지 포함해 실패하면 토스 승인 취소(보상)를 재시도하며 호출
 *  5. 불명(보류)인 주문은 PENDING + 시도 시각이 남아 PaymentReconciliationScheduler 가 재확인
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final RedissonClient redissonClient;
    private final RestClient restClient;
    private final ApplicationEventPublisher eventPublisher;

    @Lazy
    @Autowired
    private PaymentService self;

    // 생성자 시그니처를 바꾸지 않으려고 필드 주입 (없어도 동작: 로그만 남김)
    @Autowired(required = false)
    private PaymentAlertNotifier alertNotifier;

    @Value("${toss.secret-key}")
    private String tossSecretKey;

    private static final String PAYMENT_LOCK_PREFIX = "lock:payment:";
    private static final long LOCK_WAIT_TIME = 3L;
    // 최악의 경우(승인 재시도 3회 + 조회 + 보상 취소 재시도)가 락 안에서 끝나도록 넉넉하게
    private static final long LOCK_LEASE_TIME = 90L;

    private static final String TOSS_CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";
    private static final String TOSS_CANCEL_URL  = "https://api.tosspayments.com/v1/payments/{paymentKey}/cancel";
    private static final String TOSS_QUERY_BY_ORDER_URL = "https://api.tosspayments.com/v1/payments/orders/{orderId}";

    // 토스 호출 타임아웃. Nginx 기본 proxy_read_timeout(60s) 안에 끝나도록 짧게 유지한다.
    private static final int TOSS_CONNECT_TIMEOUT_MS = 3000;
    private static final int TOSS_READ_TIMEOUT_MS = 8000;

    private static final int MAX_CONFIRM_ATTEMPTS = 3;
    private static final int MAX_CANCEL_ATTEMPTS = 3;

    /** 스케줄러가 한 주문을 최대 몇 번 재확인하는지 */
    public static final int RECONCILE_MAX_CHECKS = 4;

    private static final Set<String> TOSS_FAILED_STATUSES = Set.of("ABORTED", "EXPIRED", "CANCELED");

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private record TossResponse(int httpStatus, Map<String, Object> body) {
        boolean ok() { return httpStatus >= 200 && httpStatus < 300; }
        Map<String, Object> bodyOrEmpty() { return body == null ? Map.of() : body; }
        String errorText() {
            Map<String, Object> b = bodyOrEmpty();
            return b.getOrDefault("message", "응답 없음") + " (" + b.getOrDefault("code", httpStatus) + ")";
        }
    }

    private record PreparedPayment(String orderNumber, Payment.PaymentMethod method) {}

    private enum ConfirmKind { APPROVED, REJECTED, UNKNOWN }

    private record ConfirmOutcome(ConfirmKind kind, String message) {
        static ConfirmOutcome approved() { return new ConfirmOutcome(ConfirmKind.APPROVED, null); }
        static ConfirmOutcome rejected(String m) { return new ConfirmOutcome(ConfirmKind.REJECTED, m); }
        static ConfirmOutcome unknown(String m) { return new ConfirmOutcome(ConfirmKind.UNKNOWN, m); }
    }

    public void setSelf(PaymentService self) { this.self = self; }

    // ══════════════════════════════════════════════════════════════════════════
    // 결제 승인
    // ══════════════════════════════════════════════════════════════════════════

    public PaymentResponse confirmPayment(PaymentRequest request) {
        RLock lock = redissonClient.getLock(PAYMENT_LOCK_PREFIX + request.getOrderId());
        try {
            boolean acquired = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if (!acquired) {
                throw new IllegalStateException("현재 결제가 진행 중입니다. 잠시 후 다시 시도해주세요.");
            }
            log.info("[Payment] 분산 락 획득 - orderId={}", request.getOrderId());
            return doConfirm(request);

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

    private PaymentResponse doConfirm(PaymentRequest request) {
        boolean isToss = "TOSS".equalsIgnoreCase(request.getPgProvider());
        if (isToss) {
            requireSecretKey();
        }

        // 1. 검증 + 결제 시도 기록 (짧은 트랜잭션, 토스 호출 전에 커밋)
        PreparedPayment prepared = self.preparePayment(request, isToss);

        // 2~3. 토스 승인
        if (isToss) {
            ConfirmOutcome outcome = confirmWithToss(
                    request.getPgTransactionId(), prepared.orderNumber(), request.getAmount());

            if (outcome.kind() == ConfirmKind.REJECTED) {
                self.clearPaymentRequested(request.getOrderId());   // 확정 실패: 스케줄러 대상에서 제외
                throw new IllegalStateException("토스 결제 승인 실패: " + outcome.message());
            }
            if (outcome.kind() == ConfirmKind.UNKNOWN) {
                // 주문은 PENDING + 시도 시각이 남아 있어 스케줄러가 다시 확인한다.
                log.error("[Toss] 승인 결과 불명 - orderNumber={}, paymentKey={}, reason={}",
                        prepared.orderNumber(), request.getPgTransactionId(), outcome.message());
                alert("[결제 상태 불명] orderNumber=" + prepared.orderNumber()
                        + ", paymentKey=" + request.getPgTransactionId() + " - 스케줄러가 재확인합니다.");
                throw new IllegalStateException(
                        "결제 결과를 확인하지 못했습니다. 바로 다시 주문하셔도 되며, 이전 결제가 승인되었더라도 "
                        + "자동으로 환불됩니다. 문의 시 주문번호를 알려주세요: " + prepared.orderNumber());
            }
        }

        // 4. 저장. finalizePayment 는 프록시를 거치므로 커밋 실패도 이 try 안에서 예외로 잡힌다.
        try {
            return self.finalizePayment(request, prepared.method());
        } catch (RuntimeException e) {
            if (!isToss) {
                throw e;
            }
            // 커밋은 됐는데 완료 신호만 유실된 경우를 구분하기 위해, 취소 전에 DB 를 한 번 확인한다.
            Boolean saved = isPaymentSaved(request.getPgTransactionId());
            if (Boolean.TRUE.equals(saved)) {
                log.warn("[Payment] 예외가 났지만 DB 에 결제가 저장되어 있어 취소하지 않음 - paymentKey={}, error={}",
                        request.getPgTransactionId(), e.toString());
                return self.loadPaymentResponse(request.getPgTransactionId());
            }
            if (saved == null) {
                // DB 확인 자체가 불가: 취소하면 저장됐을 때 모순이 생기므로 취소하지 않는다.
                // 저장 안 됐다면 주문이 PENDING 으로 남아 스케줄러가 DONE 을 확인하고 자동 환불한다.
                alert("[결제 저장 여부 확인 불가] orderNumber=" + prepared.orderNumber()
                        + ", paymentKey=" + request.getPgTransactionId() + " - 스케줄러가 재확인합니다.");
                throw e;
            }
            log.error("[Payment] 토스 승인 후 DB 저장 실패 → 자동 취소 시도 - paymentKey={}, error={}",
                    request.getPgTransactionId(), e.toString());
            boolean cancelled = cancelWithRetry(request.getPgTransactionId(), "서버 오류로 인한 자동 취소");
            if (!cancelled) {
                alert("[수동환불필요] 승인 후 DB 저장 실패, 자동 취소도 실패 - orderNumber="
                        + prepared.orderNumber() + ", paymentKey=" + request.getPgTransactionId());
            }
            throw e;
        }
    }

    /** 결제 저장 여부. DB 조회 자체가 실패하면 null. */
    private Boolean isPaymentSaved(String paymentKey) {
        try {
            return paymentRepository.findByPgTransactionId(paymentKey).isPresent();
        } catch (RuntimeException ex) {
            log.error("[Payment] 결제 저장 여부 확인 실패 - paymentKey={}, error={}", paymentKey, ex.toString());
            return null;
        }
    }

    @Transactional(readOnly = true)
    public PaymentResponse loadPaymentResponse(String paymentKey) {
        return paymentRepository.findByPgTransactionId(paymentKey)
                .map(PaymentResponse::from)
                .orElseThrow(() -> new IllegalStateException("결제 내역을 찾을 수 없습니다."));
    }

    @Transactional
    public PreparedPayment preparePayment(PaymentRequest request, boolean isToss) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "주문을 찾을 수 없습니다. orderId=" + request.getOrderId()));

        if (order.getStatus() == Order.OrderStatus.PAID) {
            throw new IllegalStateException("이미 결제 완료된 주문입니다. orderId=" + order.getId());
        }

        if (!order.getTotalPrice().equals(request.getAmount())) {
            throw new IllegalArgumentException(
                    "결제 금액이 주문 금액과 다릅니다. 요청=" + request.getAmount()
                    + ", 주문=" + order.getTotalPrice());
        }

        if (paymentRepository.findByPgTransactionId(request.getPgTransactionId()).isPresent()) {
            throw new IllegalStateException(
                    "이미 사용된 거래 ID입니다. pgTransactionId=" + request.getPgTransactionId());
        }

        Payment.PaymentMethod method;
        try {
            method = Payment.PaymentMethod.valueOf(request.getMethod().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("지원하지 않는 결제 수단입니다: " + request.getMethod());
        }

        if (isToss) {
            order.markPaymentRequested();   // 토스 호출 전에 커밋되어, 도중에 죽어도 스케줄러가 찾을 수 있다.
        }
        return new PreparedPayment(order.getOrderNumber(), method);
    }

    @Transactional
    public PaymentResponse finalizePayment(PaymentRequest request, Payment.PaymentMethod method) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "주문을 찾을 수 없습니다. orderId=" + request.getOrderId()));

        if (order.getStatus() == Order.OrderStatus.PAID) {
            throw new IllegalStateException("이미 결제 완료된 주문입니다. orderId=" + order.getId());
        }

        Payment payment = Payment.builder()
                .order(order)
                .pgProvider(request.getPgProvider().toUpperCase())
                .pgTransactionId(request.getPgTransactionId())
                .method(method)
                .amount(request.getAmount())
                .paidAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        order.changeStatus(Order.OrderStatus.PAID);
        eventPublisher.publishEvent(new OrderPaidEvent(OrderEvent.from(order)));
        return PaymentResponse.from(payment);
    }

    @Transactional
    public void clearPaymentRequested(Long orderId) {
        orderRepository.findById(orderId).ifPresent(Order::clearPaymentRequested);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 토스 승인 호출 (재시도 + 조회 확인)
    // ══════════════════════════════════════════════════════════════════════════

    private ConfirmOutcome confirmWithToss(String paymentKey, String tossOrderId, Integer amount) {
        Map<String, Object> body = Map.of(
                "paymentKey", paymentKey, "orderId", tossOrderId, "amount", amount);

        for (int attempt = 1; attempt <= MAX_CONFIRM_ATTEMPTS; attempt++) {
            try {
                // 같은 Idempotency-Key(paymentKey) → 재시도해도 토스가 같은 결제로 처리한다.
                TossResponse res = callToss(HttpMethod.POST, TOSS_CONFIRM_URL, body, paymentKey);

                if (res.ok()) {
                    return validateApproved(paymentKey, tossOrderId, amount, res.bodyOrEmpty());
                }
                int status = res.httpStatus();
                if (status != 409 && status < 500) {
                    // 4xx: 토스가 명확히 거절 (카드 한도 초과, 키 오류 등)
                    log.error("[Toss] 결제 승인 거절 - paymentKey={}, {}", paymentKey, res.errorText());
                    return ConfirmOutcome.rejected(res.errorText());
                }
                log.warn("[Toss] 승인 재시도 필요 - attempt={}, http={}, {}", attempt, status, res.errorText());
            } catch (ResourceAccessException e) {
                log.warn("[Toss] 승인 응답 없음(연결/시간 초과) - attempt={}, {}", attempt, e.getMessage());
            }
            if (attempt < MAX_CONFIRM_ATTEMPTS) {
                sleepQuietly(attempt * 1000L);
            }
        }

        // 재시도로도 결과를 못 받음 → 토스에 실제 상태를 물어본다.
        return queryOutcome(paymentKey, tossOrderId, amount);
    }

    private ConfirmOutcome validateApproved(String paymentKey, String tossOrderId, Integer amount,
                                            Map<String, Object> body) {
        String status = String.valueOf(body.get("status"));
        if (!"DONE".equals(status)) {
            // 카드 외 결제수단(가상계좌 등)은 지원하지 않는다.
            return ConfirmOutcome.rejected("결제가 완료되지 않았습니다. status=" + status);
        }
        Object total = body.get("totalAmount");
        boolean amountOk = total instanceof Number n && n.intValue() == amount;
        boolean orderOk = tossOrderId.equals(body.get("orderId"));
        if (!amountOk || !orderOk) {
            log.error("[Toss] 승인 응답 불일치 - paymentKey={}, totalAmount={}, orderId={}",
                    paymentKey, total, body.get("orderId"));
            cancelWithRetry(paymentKey, "승인 응답 불일치로 인한 자동 취소");
            return ConfirmOutcome.rejected("승인 응답이 주문 정보와 일치하지 않아 취소했습니다.");
        }
        log.info("[Toss] 결제 승인 성공 - paymentKey={}, orderId={}", paymentKey, tossOrderId);
        return ConfirmOutcome.approved();
    }

    private ConfirmOutcome queryOutcome(String paymentKey, String tossOrderId, Integer amount) {
        TossResponse res;
        try {
            res = callToss(HttpMethod.GET, TOSS_QUERY_BY_ORDER_URL, null, null, tossOrderId);
        } catch (ResourceAccessException e) {
            return ConfirmOutcome.unknown("토스 조회도 응답 없음");
        }
        if (!res.ok()) {
            // 404(결제 없음)도 "아직 처리 전"일 수 있어 실패로 단정하지 않는다.
            return ConfirmOutcome.unknown("토스 조회 실패: " + res.errorText());
        }
        Map<String, Object> body = res.bodyOrEmpty();
        String status = String.valueOf(body.get("status"));

        if ("DONE".equals(status)) {
            Object total = body.get("totalAmount");
            boolean amountOk = total instanceof Number n && n.intValue() == amount;
            boolean keyOk = paymentKey.equals(body.get("paymentKey"));
            if (amountOk && keyOk) {
                log.info("[Toss] 조회로 승인 확인 - paymentKey={}", paymentKey);
                return ConfirmOutcome.approved();
            }
            return ConfirmOutcome.unknown("조회 결과가 요청과 다름(paymentKey/금액)");
        }
        if (TOSS_FAILED_STATUSES.contains(status)) {
            return ConfirmOutcome.rejected("결제가 완료되지 않았습니다. status=" + status);
        }
        return ConfirmOutcome.unknown("토스 결제 상태=" + status);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 결제 취소 / 환불
    // ══════════════════════════════════════════════════════════════════════════

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

        if ("TOSS".equalsIgnoreCase(payment.getPgProvider())) {
            TossResponse res = tossCancel(payment.getPgTransactionId(), request.getCancelReason());
            if (!res.ok()) {
                log.error("[Toss] 결제 취소 실패 - paymentKey={}, {}", payment.getPgTransactionId(), res.errorText());
                throw new IllegalStateException("토스 결제 취소 실패: " + res.errorText());
            }
        }

        order.changeStatus(Order.OrderStatus.REFUNDED);
        return PaymentCancelResponse.from(payment, request.getCancelReason());
    }

    /** 취소 호출 + 재시도 (409/5xx/무응답만). 취소 전용 Idempotency-Key 사용. */
    private TossResponse tossCancel(String paymentKey, String reason) {
        TossResponse last = new TossResponse(-1, null);
        for (int attempt = 1; attempt <= MAX_CANCEL_ATTEMPTS; attempt++) {
            try {
                last = callToss(HttpMethod.POST, TOSS_CANCEL_URL, Map.of("cancelReason", reason),
                        "cancel-" + paymentKey, paymentKey);
                if (last.ok()) {
                    log.info("[Toss] 결제 취소 성공 - paymentKey={}", paymentKey);
                    return last;
                }
                if (last.httpStatus() != 409 && last.httpStatus() < 500) {
                    return last;   // 4xx: 재시도해도 소용없음
                }
            } catch (ResourceAccessException e) {
                log.warn("[Toss] 취소 응답 없음 - attempt={}, {}", attempt, e.getMessage());
                last = new TossResponse(-1, null);
            }
            if (attempt < MAX_CANCEL_ATTEMPTS) {
                sleepQuietly(attempt * 1000L);
            }
        }
        return last;
    }

    private boolean cancelWithRetry(String paymentKey, String reason) {
        TossResponse res = tossCancel(paymentKey, reason);
        if (!res.ok()) {
            log.error("[Toss] 자동 취소 최종 실패 - paymentKey={}, {}", paymentKey, res.errorText());
        }
        return res.ok();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 정산 대조 (스케줄러에서 호출): PENDING + 결제 시도 흔적이 있는 주문 재확인
    // ══════════════════════════════════════════════════════════════════════════

    public void reconcile(Long orderId) {
        RLock lock = redissonClient.getLock(PAYMENT_LOCK_PREFIX + orderId);
        try {
            if (!lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                log.info("[Reconcile] 결제 진행 중이라 건너뜀 - orderId={}", orderId);
                return;
            }
            reconcileLocked(orderId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void reconcileLocked(Long orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || order.getStatus() != Order.OrderStatus.PENDING) {
            return;   // 그 사이 결제 완료/취소됨
        }

        TossResponse res;
        try {
            res = callToss(HttpMethod.GET, TOSS_QUERY_BY_ORDER_URL, null, null, order.getOrderNumber());
        } catch (ResourceAccessException e) {
            res = new TossResponse(-1, null);
        }
        Map<String, Object> body = res.bodyOrEmpty();
        String status = res.ok() ? String.valueOf(body.get("status")) : "";

        if ("DONE".equals(status)) {
            // 뒤늦게 승인된 결제: 키오스크 특성상 지금 주방으로 보내지 않고 자동 환불한다.
            refundLatePayment(order, body);
            return;
        }
        if (TOSS_FAILED_STATUSES.contains(status)) {
            self.markCancelled(orderId);
            log.info("[Reconcile] 결제 미완료 확정으로 주문 취소 - orderNumber={}, status={}",
                    order.getOrderNumber(), status);
            return;
        }

        int count = self.recordCheck(orderId);
        log.warn("[Reconcile] 결제 상태 미확정 - orderNumber={}, status={}, count={}/{}",
                order.getOrderNumber(), res.ok() ? status : res.errorText(), count, RECONCILE_MAX_CHECKS);
        if (count >= RECONCILE_MAX_CHECKS) {
            alert("[결제 상태 확인 불가] 재확인 " + count + "회 후에도 미확정 - orderNumber="
                    + order.getOrderNumber() + ", 토스 결제내역 수동 확인 필요");
        }
    }

    private void refundLatePayment(Order order, Map<String, Object> body) {
        String paymentKey = String.valueOf(body.get("paymentKey"));
        Object total = body.get("totalAmount");
        int amount = total instanceof Number n ? n.intValue() : order.getTotalPrice();

        boolean cancelled = cancelWithRetry(paymentKey, "결제 지연 확인으로 인한 자동 환불");
        if (cancelled) {
            self.recordLateRefund(order.getId(), paymentKey, amount);
            log.warn("[Reconcile] 지연 승인 결제 자동 환불 완료 - orderNumber={}, paymentKey={}",
                    order.getOrderNumber(), paymentKey);
        } else {
            int count = self.recordCheck(order.getId());
            alert("[수동환불필요] 지연 승인 결제 자동 환불 실패 - orderNumber=" + order.getOrderNumber()
                    + ", paymentKey=" + paymentKey + ", 재확인 " + count + "/" + RECONCILE_MAX_CHECKS);
        }
    }

    @Transactional
    public void markCancelled(Long orderId) {
        orderRepository.findById(orderId).ifPresent(o -> {
            if (o.getStatus() == Order.OrderStatus.PENDING) {
                o.changeStatus(Order.OrderStatus.CANCELLED);
            }
        });
    }

    @Transactional
    public int recordCheck(Long orderId) {
        return orderRepository.findById(orderId).map(Order::recordPaymentCheck).orElse(0);
    }

    @Transactional
    public void recordLateRefund(Long orderId, String paymentKey, int amount) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. orderId=" + orderId));
        Payment payment = Payment.builder()
                .order(order)
                .pgProvider("TOSS")
                .pgTransactionId(paymentKey)
                .method(Payment.PaymentMethod.CARD)
                .amount(amount)
                .paidAt(LocalDateTime.now())
                .build();
        paymentRepository.save(payment);
        order.changeStatus(Order.OrderStatus.REFUNDED);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 공통
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * 토스 API 공통 호출. 4xx/5xx 도 예외 없이 응답(code, message)을 돌려준다.
     * 연결/응답 시간 초과는 ResourceAccessException 으로 던진다.
     */
    private TossResponse callToss(HttpMethod method, String uriTemplate, Object body,
                                  String idempotencyKey, Object... uriVars) {
        requireSecretKey();

        String authorization = "Basic " + Base64.getEncoder()
                .encodeToString((tossSecretKey + ":").getBytes(StandardCharsets.UTF_8));

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(TOSS_CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(TOSS_READ_TIMEOUT_MS);

        RestClient.RequestBodySpec spec = restClient.mutate().requestFactory(factory).build()
                .method(method)
                .uri(uriTemplate, uriVars)
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON);
        if (idempotencyKey != null) {
            spec = spec.header("Idempotency-Key", idempotencyKey);
        }
        if (body != null) {
            spec = spec.body(body);
        }

        return spec.exchange((request, res) -> {
            int status = res.getStatusCode().value();
            Map<String, Object> responseBody;
            try {
                responseBody = res.bodyTo(MAP_TYPE);
            } catch (RuntimeException e) {
                responseBody = null;   // JSON 이 아닌 응답
            }
            return new TossResponse(status, responseBody);
        });
    }

    private void requireSecretKey() {
        if (tossSecretKey == null || tossSecretKey.isBlank()) {
            throw new IllegalStateException("토스 시크릿 키가 설정되지 않았습니다. (TOSS_SECRET_KEY)");
        }
    }

    private void alert(String message) {
        if (alertNotifier != null) {
            alertNotifier.send(message);
        } else {
            log.error("[ALERT] {}", message);
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}