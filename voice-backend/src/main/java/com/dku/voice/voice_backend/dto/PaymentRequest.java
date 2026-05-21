package com.dku.voice.voice_backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    // PG사 구분 (TOSS, KAKAO, NAVER 등)
    @NotBlank(message = "pgProvider는 필수입니다.")
    private String pgProvider;

    // PG사별 거래 고유번호 (토스: paymentKey, 카카오: tid, 네이버: paymentId)
    @NotBlank(message = "pgTransactionId는 필수입니다.")
    private String pgTransactionId;

    // 프론트에서 전달하는 주문 ID (위변조 검증 대상)
    @NotNull(message = "orderId는 필수입니다.")
    private Long orderId;

    // 프론트에서 전달하는 결제 금액 (위변조 검증 대상)
    @NotNull(message = "amount는 필수입니다.")
    @Min(value = 1, message = "결제 금액은 1원 이상이어야 합니다.")
    private Integer amount;

    @NotBlank(message = "결제 수단은 필수입니다.")
    private String method;
}