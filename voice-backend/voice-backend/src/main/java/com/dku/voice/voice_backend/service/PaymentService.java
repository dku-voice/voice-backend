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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    /**
     * 결제 승인 (2주차)
     * 1. 주문 조회
     * 2. 위변조 검증: 요청 금액 vs DB 주문 금액 비교
     * 3. 중복 결제 검증: pgTransactionId 중복 확인
     * 4. Payment 저장 + Order 상태 PAID 변경
     */
    @Transactional
    public PaymentResponse confirmPayment(PaymentRequest request) {

        // 1. 주문 조회
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "주문을 찾을 수 없습니다. orderId=" + request.getOrderId()));

        // 2. 이미 결제된 주문인지 확인 (Order.status 기준)
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
            throw new IllegalStateException("이미 사용된 거래 ID입니다. pgTransactionId=" + request.getPgTransactionId());
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