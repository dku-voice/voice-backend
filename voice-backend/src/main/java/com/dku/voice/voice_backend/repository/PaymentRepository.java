package com.dku.voice.voice_backend.repository;

import com.dku.voice.voice_backend.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * PG사 거래 고유번호로 조회
     * - 결제 위변조 검증 및 중복 결제 차단 (2주차)
     * - 환불/취소 처리 (3주차)
     * - PG사(TOSS, KAKAO, NAVER 등)에 무관하게 동일하게 사용
     */
    Optional<Payment> findByPgTransactionId(String pgTransactionId);

    /**
     * 주문 ID로 결제 조회
     * - 주문-결제 1:1 관계 조회
     */
    @Query("""
        SELECT p
        FROM Payment p
        JOIN FETCH p.order o
        WHERE o.id = :orderId
        """)
    Optional<Payment> findByOrderId(@Param("orderId") Long orderId);
}