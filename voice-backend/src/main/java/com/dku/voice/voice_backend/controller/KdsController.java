package com.dku.voice.voice_backend.controller;

import com.dku.voice.voice_backend.dto.ApiResponse;
import com.dku.voice.voice_backend.dto.OrderResponse;
import com.dku.voice.voice_backend.entity.Order;
import com.dku.voice.voice_backend.repository.OrderRepository;
import com.dku.voice.voice_backend.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/kds")
@RequiredArgsConstructor
public class KdsController {

    private final SseEmitterService sseEmitterService;
    private final OrderRepository orderRepository;

    /**
     * GET /kds/stream?storeId={storeId}
     * KDS 화면이 서버에 SSE 연결 요청
     * - SseEmitter 생성 및 storeId 기준으로 등록
     * - 이후 새 주문 발생 시 이 연결로 실시간 전송
     * - JWT 인증 필요 (SecurityConfig에서 /kds/** 설정)
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam String storeId) {
        log.info("[KDS] SSE 연결 요청 - storeId={}", storeId);
        return sseEmitterService.connect(storeId);
    }

    /**
     * GET /kds/orders?storeId={storeId}
     * KDS 화면 첫 로딩 시 현재 대기 중인 주문 목록 조회
     * - PAID, PREPARING 상태 주문만 반환
     * - SSE 연결 전 기존 주문을 화면에 채우기 위해 필요
     */
    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrders(
            @RequestParam String storeId) {

        List<OrderResponse> orders = orderRepository
                .findByStoreIdAndStatusIn(
                        storeId,
                        List.of(Order.OrderStatus.PAID, Order.OrderStatus.PREPARING))
                .stream()
                .map(OrderResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    /**
     * PATCH /kds/orders/{orderId}/status
     * 주방 직원이 조리 시작/완료 버튼 클릭 시 호출
     * - PAID → PREPARING (조리 시작)
     * - PREPARING → COMPLETED (조리 완료)
     */
    @PatchMapping("/orders/{orderId}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable Long orderId,
            @RequestParam Order.OrderStatus status) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "주문을 찾을 수 없습니다. orderId=" + orderId));

        order.changeStatus(status);
        orderRepository.save(order);

        log.info("[KDS] 주문 상태 변경 - orderId={}, status={}", orderId, status);
        return ResponseEntity.ok(ApiResponse.success(OrderResponse.from(order)));
    }
}