package com.dku.voice.voice_backend.controller;

import com.dku.voice.voice_backend.dto.ApiResponse;
import com.dku.voice.voice_backend.dto.PaymentCancelRequest;
import com.dku.voice.voice_backend.dto.PaymentCancelResponse;
import com.dku.voice.voice_backend.dto.PaymentRequest;
import com.dku.voice.voice_backend.dto.PaymentResponse;
import com.dku.voice.voice_backend.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 결제 승인
     * POST /api/payments/confirm
     */
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmPayment(
            @RequestBody @Valid PaymentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.confirmPayment(request)));
    }

    /**
     * 결제 취소 / 환불
     * POST /api/payments/cancel
     */
    @PostMapping("/cancel")
    public ResponseEntity<ApiResponse<PaymentCancelResponse>> cancelPayment(
            @RequestBody @Valid PaymentCancelRequest request) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.cancelPayment(request)));
    }
}