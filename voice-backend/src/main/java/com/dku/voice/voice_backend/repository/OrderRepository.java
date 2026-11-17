package com.dku.voice.voice_backend.repository;

import com.dku.voice.voice_backend.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * KDS 화면 첫 로딩 시 대기 중인 주문 목록 조회
     */
    List<Order> findByStoreIdAndStatusIn(String storeId, List<Order.OrderStatus> statuses);

    /**
     * 매장별 전체 주문 조회 (관리자용)
     */
    List<Order> findByStoreId(String storeId);

    /**
     * 주문번호로 단건 조회
     */
    Optional<Order> findByOrderNumber(String orderNumber);

        /**
     * 주문 + 아이템 + 메뉴 조회 (영수증 출력용)
     * 컬렉션 fetch join은 하나만 가능하므로 options는 지연 로딩(@BatchSize)으로 가져온다.
     */
    @Query("""
        SELECT DISTINCT o
        FROM Order o
        JOIN FETCH o.orderItems oi
        JOIN FETCH oi.menu
        WHERE o.id = :orderId
        """)
    Optional<Order> findByIdWithItems(@Param("orderId") Long orderId);

    /**
     * 상태별 주문 조회 - KDS 전달용
     */
    @Query("""
        SELECT DISTINCT o
        FROM Order o
        JOIN FETCH o.orderItems oi
        JOIN FETCH oi.menu
        WHERE o.status = :status
        ORDER BY o.orderedAt ASC
        """)
    List<Order> findByStatusWithItemsOrderByOrderedAtAsc(@Param("status") Order.OrderStatus status);

    /**
     * 인기 메뉴 통계 (관리자용)
     * - 기간별 메뉴별 주문 수량 합계
     * - 내림차순 정렬
     */
    @Query("""
        SELECT oi.menu.nameKo, SUM(oi.quantity) as totalQuantity
        FROM OrderItem oi
        JOIN oi.order o
        WHERE o.storeId = :storeId
          AND o.orderedAt >= :from
          AND o.orderedAt < :to
          AND o.status NOT IN ('CANCELLED', 'REFUNDED')
        GROUP BY oi.menu.id, oi.menu.nameKo
        ORDER BY totalQuantity DESC
        """)
    List<Object[]> findMenuStats(
            @Param("storeId") String storeId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    /**
     * 결제 재확인 대상 조회 (정산 대조 스케줄러용)
     * - 결제 승인을 시도했지만 결과를 확인하지 못해 아직 PENDING인 주문
     * - 마지막 시도/확인 후 threshold 이전이고, 재확인 횟수가 maxCount 미만인 것
     */
    @Query("""
        SELECT o
        FROM Order o
        WHERE o.status = :status
          AND o.paymentRequestedAt IS NOT NULL
          AND o.paymentCheckCount < :maxCount
          AND COALESCE(o.paymentCheckedAt, o.paymentRequestedAt) < :threshold
        """)
    List<Order> findPaymentCheckTargets(
            @Param("status") Order.OrderStatus status,
            @Param("maxCount") int maxCount,
            @Param("threshold") LocalDateTime threshold
    );
}