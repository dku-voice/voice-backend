package com.dku.voice.voice_backend.controller;

import com.dku.voice.voice_backend.dto.ApiResponse;
import com.dku.voice.voice_backend.dto.PaymentResponse;
import com.dku.voice.voice_backend.entity.Payment;
import com.dku.voice.voice_backend.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/admin/payments")
@RequiredArgsConstructor
public class AdminPaymentController {

    private final PaymentRepository paymentRepository;

    /**
     * GET /admin/payments
     * 결제 내역 조회
     * - storeId 기준 해당 매장 결제만
     * - 날짜 필터 (선택)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPayments(
            @RequestParam String storeId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<Payment> payments = paymentRepository.findByStoreId(storeId);

        // 날짜 필터
        if (date != null) {
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            payments = payments.stream()
                    .filter(p -> p.getPaidAt().isAfter(start)
                            && p.getPaidAt().isBefore(end))
                    .collect(Collectors.toList());
        }

        return ResponseEntity.ok(ApiResponse.success(
                payments.stream().map(PaymentResponse::from).collect(Collectors.toList())));
    }
}