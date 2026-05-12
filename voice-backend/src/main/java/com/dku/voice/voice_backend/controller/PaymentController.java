package com.dku.voice.voice_backend.controller;

import com.dku.voice.voice_backend.dto.PaymentCancelRequest;
import com.dku.voice.voice_backend.dto.PaymentCancelResponse;
import com.dku.voice.voice_backend.dto.PaymentRequest;
import com.dku.voice.voice_backend.dto.PaymentResponse;
import com.dku.voice.voice_backend.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 결제 승인 (2주차)
     * POST /api/payments/confirm
     * - 위변조 검증 후 결제 완료 처리
     */
    @PostMapping("/confirm")
    public ResponseEntity<PaymentResponse> confirmPayment(@RequestBody @Valid PaymentRequest request) {
        return ResponseEntity.ok(paymentService.confirmPayment(request));
    }

    /**
     * 결제 취소 / 환불 (3주차)
     * POST /api/payments/cancel
     * - 결제 상태 CANCELLED + 주문 상태 REFUNDED
     */
    @PostMapping("/cancel")
    public ResponseEntity<PaymentCancelResponse> cancelPayment(@RequestBody @Valid PaymentCancelRequest request) {
        return ResponseEntity.ok(paymentService.cancelPayment(request));
    }
}