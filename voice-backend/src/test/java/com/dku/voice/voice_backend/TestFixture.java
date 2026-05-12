package com.dku.voice.voice_backend;

import com.dku.voice.voice_backend.dto.*;
import com.dku.voice.voice_backend.entity.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 테스트 전용 픽스처 헬퍼
 * - 테스트마다 반복되는 객체 생성 코드를 중앙 관리
 * - 프로덕션 코드에 영향 없음
 */
public class TestFixture {

    // ── Menu ────────────────────────────────────────────────────────────────

    public static Menu activeMenu() {
        return Menu.builder()
                .id(1L)
                .nameKo("불고기버거")
                .nameEn("Bulgogi Burger")
                .price(6000)
                .status(Menu.MenuStatus.ACTIVE)
                .build();
    }

    public static Menu activeMenu(Long id, String nameKo, int price) {
        return Menu.builder()
                .id(id)
                .nameKo(nameKo)
                .nameEn(nameKo)
                .price(price)
                .status(Menu.MenuStatus.ACTIVE)
                .build();
    }

    public static Menu unavailableMenu(Long id, Menu.MenuStatus status) {
        return Menu.builder()
                .id(id)
                .nameKo("주문불가메뉴")
                .nameEn("Unavailable Menu")
                .price(5000)
                .status(status)
                .build();
    }

    // ── Order ───────────────────────────────────────────────────────────────

    public static Order pendingOrder() {
        return Order.builder()
                .id(1001L)
                .orderNumber("ORD-20260511-ABCD1234")
                .totalPrice(12000)
                .status(Order.OrderStatus.PENDING)
                .build();
    }

    public static Order paidOrder() {
        return Order.builder()
                .id(1002L)
                .orderNumber("ORD-20260511-EFGH5678")
                .totalPrice(12000)
                .status(Order.OrderStatus.PAID)
                .build();
    }

    // ── Payment ─────────────────────────────────────────────────────────────

    public static Payment payment(String pgTransactionId, Order order) {
        return Payment.builder()
                .id(1L)
                .order(order)
                .pgProvider("TOSS")
                .pgTransactionId(pgTransactionId)
                .method(Payment.PaymentMethod.TOSS)
                .amount(order.getTotalPrice())
                .paidAt(LocalDateTime.now())
                .build();
    }

    // ── Request DTO ─────────────────────────────────────────────────────────

    public static OrderRequest orderRequest(long[] menuIds, int[] quantities) {
        List<OrderItemRequest> items = new java.util.ArrayList<>();
        for (int i = 0; i < menuIds.length; i++) {
            items.add(OrderItemRequest.builder()
                    .menuId(menuIds[i])
                    .quantity(quantities[i])
                    .build());
        }
        return OrderRequest.builder()
                .items(items)
                .build();
    }

    public static PaymentRequest paymentRequest(String pgTransactionId, Long orderId, int amount) {
        return PaymentRequest.builder()
                .pgProvider("TOSS")
                .pgTransactionId(pgTransactionId)
                .orderId(orderId)
                .amount(amount)
                .method("TOSS")
                .build();
    }

    public static PaymentCancelRequest cancelRequest(String pgTransactionId, String reason) {
        return PaymentCancelRequest.builder()
                .pgTransactionId(pgTransactionId)
                .cancelReason(reason)
                .build();
    }
}