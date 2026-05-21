package com.dku.voice.voice_backend.dto;

import com.dku.voice.voice_backend.entity.Payment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PaymentResponse {

    private Long paymentId;
    private Long orderId;
    private String pgProvider;
    private String pgTransactionId;
    private String method;
    private Integer amount;
    private LocalDateTime paidAt;

    public static PaymentResponse from(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrder().getId())
                .pgProvider(payment.getPgProvider())
                .pgTransactionId(payment.getPgTransactionId())
                .method(payment.getMethod().name())
                .amount(payment.getAmount())
                .paidAt(payment.getPaidAt())
                .build();
    }
}