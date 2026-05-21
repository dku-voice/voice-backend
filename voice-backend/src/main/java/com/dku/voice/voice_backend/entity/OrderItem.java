package com.dku.voice.voice_backend.entity;

import jakarta.persistence.*;
import lombok.*;
 
@Entity
@Table(name = "order_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class OrderItem {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", nullable = false)
    private Menu menu;
 
    @Column(nullable = false)
    private Integer quantity;
 
    @Column(name = "unit_price", nullable = false)
    private Integer unitPrice;
}
