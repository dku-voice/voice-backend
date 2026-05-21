package com.dku.voice.voice_backend.repository;

import com.dku.voice.voice_backend.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
 
public interface OrderRepository extends JpaRepository<Order, Long> {
 
    /**
     * 주문번호로 단건 조회 (결제 검증 시 사용)
     */
    Optional<Order> findByOrderNumber(String orderNumber);
 
    /**
     * 주문 + 아이템 + 메뉴 한 번에 조회 (영수증 출력용)
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
     * 상태별 주문 조회 - 주방 디스플레이(KDS) 전달용
     * PENDING(대기) 주문을 접수 시간 오름차순으로 조회
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
}