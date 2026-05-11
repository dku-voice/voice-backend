package com.dku.voice.voice_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCancelRequest {

    // PG사 거래 고유번호 (토스: paymentKey, 카카오: tid, 네이버: paymentId)
    @NotBlank(message = "pgTransactionId는 필수입니다.")
    private String pgTransactionId;

    @NotBlank(message = "취소 사유는 필수입니다.")
    private String cancelReason;
}