package com.dku.voice.voice_backend.repository;

import com.dku.voice.voice_backend.dto.SalesStatsResponse;
import com.dku.voice.voice_backend.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * PG사 거래 고유번호로 조회
     */
    Optional<Payment> findByPgTransactionId(String pgTransactionId);

    /**
     * 주문 ID로 결제 조회
     */
    @Query("""
        SELECT p
        FROM Payment p
        JOIN FETCH p.order o
        WHERE o.id = :orderId
        """)
    Optional<Payment> findByOrderId(@Param("orderId") Long orderId);

    /**
     * 매장별 결제 내역 조회 (관리자용)
     */
    @Query("""
        SELECT p
        FROM Payment p
        JOIN FETCH p.order o
        WHERE o.storeId = :storeId
        ORDER BY p.paidAt DESC
        """)
    List<Payment> findByStoreId(@Param("storeId") String storeId);

    /**
     * 매출 통계 (관리자용)
     * - 기간별 총 매출, 총 주문 수, 평균 주문 금액
     */
    @Query("""
        SELECT new com.dku.voice.voice_backend.dto.SalesStatsResponse(
            CAST(SUM(p.amount) AS integer),
            COUNT(p),
            CAST(AVG(p.amount) AS integer)
        )
        FROM Payment p
        JOIN p.order o
        WHERE o.storeId = :storeId
          AND p.paidAt >= :from
          AND p.paidAt < :to
        """)
    SalesStatsResponse findSalesStats(
            @Param("storeId") String storeId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}