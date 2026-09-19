package com.dku.voice.voice_backend.controller;

import com.dku.voice.voice_backend.dto.ApiResponse;
import com.dku.voice.voice_backend.dto.OrderResponse;
import com.dku.voice.voice_backend.entity.Order;
import com.dku.voice.voice_backend.repository.OrderRepository;
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
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderRepository orderRepository;

    /**
     * GET /admin/orders
     * 전체 주문 조회
     * - storeId 기준 해당 매장 주문만
     * - 날짜 필터 (선택)
     * - 상태 필터 (선택)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrders(
            @RequestParam String storeId,
            @RequestParam(required = false) Order.OrderStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<Order> orders;

        if (status != null) {
            orders = orderRepository.findByStoreIdAndStatusIn(storeId, List.of(status));
        } else {
            orders = orderRepository.findByStoreId(storeId);
        }

        // 날짜 필터
        if (date != null) {
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            orders = orders.stream()
                    .filter(o -> o.getOrderedAt().isAfter(start)
                            && o.getOrderedAt().isBefore(end))
                    .collect(Collectors.toList());
        }

        return ResponseEntity.ok(ApiResponse.success(
                orders.stream().map(OrderResponse::from).collect(Collectors.toList())));
    }

    /**
     * GET /admin/orders/{orderId}
     * 주문 상세 조회
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable Long orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "주문을 찾을 수 없습니다. orderId=" + orderId));

        return ResponseEntity.ok(ApiResponse.success(OrderResponse.from(order)));
    }
}