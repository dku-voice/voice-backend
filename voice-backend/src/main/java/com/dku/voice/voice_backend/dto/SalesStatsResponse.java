package com.dku.voice.voice_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesStatsResponse {

    private Integer totalSales;     // 총 매출
    private Long totalOrders;       // 총 주문 수
    private Integer averageOrder;   // 평균 주문 금액
}