package com.dku.voice.voice_backend.controller;

import com.dku.voice.voice_backend.dto.ApiResponse;
import com.dku.voice.voice_backend.dto.SalesStatsResponse;
import com.dku.voice.voice_backend.repository.OrderRepository;
import com.dku.voice.voice_backend.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    /**
     * GET /admin/stats/sales?storeId=&from=&to=
     * 매출 통계
     * - 기간별 총 매출
     * - 총 주문 수
     * - 평균 주문 금액
     */
    @GetMapping("/sales")
    public ResponseEntity<ApiResponse<SalesStatsResponse>> getSalesStats(
            @RequestParam String storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        SalesStatsResponse stats = paymentRepository.findSalesStats(
                storeId,
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay()
        );

        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * GET /admin/stats/menus?storeId=&from=&to=
     * 인기 메뉴 통계
     * - 기간별 메뉴별 주문 수량
     * - 내림차순 정렬
     */
    @GetMapping("/menus")
    public ResponseEntity<ApiResponse<?>> getMenuStats(
            @RequestParam String storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        var stats = orderRepository.findMenuStats(
                storeId,
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay()
        );

        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}