package com.dku.voice.voice_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    /**
     * 매장 ID - 다중 매장 구분용
     * KDS SSE 연결 시 storeId 기준으로 해당 매장 KDS에만 전송
     */
    @Column(name = "store_id", nullable = false, length = 50)
    @Builder.Default
    private String storeId = "store-default";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "total_price", nullable = false)
    private Integer totalPrice;

    @Column(name = "ordered_at", nullable = false)
    @Builder.Default
    private LocalDateTime orderedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    @OneToOne(mappedBy = "order", fetch = FetchType.LAZY)
    private Payment payment;

    public void changeStatus(OrderStatus status) {
        this.status = status;
    }

    public enum OrderStatus {
        PENDING,    // 주문 대기 (결제 전)
        PAID,       // 결제 완료 → KDS로 전송
        PREPARING,  // 조리 중 (KDS에서 변경)
        COMPLETED,  // 조리 완료 (KDS에서 변경)
        CANCELLED,  // 취소
        REFUNDED    // 환불
    }
}