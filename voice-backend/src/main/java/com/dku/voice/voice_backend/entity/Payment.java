package com.dku.voice.voice_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
 
@Entity
@Table(name = "payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Payment {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;
 
    @Column(name = "pg_provider")         // TOSS, KAKAO, NAVER
    private String pgProvider;

    @Column(name = "pg_transaction_id")   // 각 PG사의 고유 키
    private String pgTransactionId;
 
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;
 
    @Column(nullable = false)
    private Integer amount;
 
    @Column(name = "paid_at")
    private LocalDateTime paidAt;
 
 
    public enum PaymentMethod {
        CARD, CASH, KAKAO_PAY, NAVER_PAY, TOSS
    }
}