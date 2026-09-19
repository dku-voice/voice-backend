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
     * 주문 + 아이템 + 메뉴 + 선택 옵션 한 번에 조회 (영수증 출력용)
     */
    @Query("""
        SELECT DISTINCT o
        FROM Order o
        JOIN FETCH o.orderItems oi
        JOIN FETCH oi.menu
        LEFT JOIN FETCH oi.options oio
        LEFT JOIN FETCH oio.menuOption
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
        LEFT JOIN FETCH oi.options oio
        LEFT JOIN FETCH oio.menuOption
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
}