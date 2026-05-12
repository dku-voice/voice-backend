package com.dku.voice.voice_backend.dto;

import com.dku.voice.voice_backend.entity.Payment;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentCancelResponse {

    private Long paymentId;
    private Long orderId;
    private String pgProvider;
    private String pgTransactionId;
    private String cancelReason;
    private Integer refundAmount;
    private String orderStatus;

    public static PaymentCancelResponse from(Payment payment, String cancelReason) {
        return PaymentCancelResponse.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrder().getId())
                .pgProvider(payment.getPgProvider())
                .pgTransactionId(payment.getPgTransactionId())
                .cancelReason(cancelReason)
                .refundAmount(payment.getAmount())
                .orderStatus(payment.getOrder().getStatus().name())
                .build();
    }
}