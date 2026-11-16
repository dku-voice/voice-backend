package com.dku.voice.voice_backend.controller;

import com.dku.voice.voice_backend.dto.ApiResponse;
import com.dku.voice.voice_backend.dto.OrderRequest;
import com.dku.voice.voice_backend.dto.OrderResponse;
import com.dku.voice.voice_backend.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 주문 생성 (장바구니 → 주문 확정)
     * POST /api/orders
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @RequestBody @Valid OrderRequest request) {
        return ResponseEntity.ok(ApiResponse.success(orderService.createOrder(request)));
    }

    /**
     * 영수증 조회
     * GET /api/orders/{orderId}
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrder(orderId)));
    }
}