package com.dku.voice.voice_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "menu_nutrients")
public class MenuNutrient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", nullable = false, unique = true)
    private Menu menu;

    @Column(name = "calories")
    private Integer calories;

    @Column(name = "protein", precision = 5, scale = 1)
    private BigDecimal protein;

    @Column(name = "fat", precision = 5, scale = 1)
    private BigDecimal fat;

    @Column(name = "carbs", precision = 5, scale = 1)
    private BigDecimal carbs;

    @Column(name = "sodium")
    private Integer sodium;

    @Column(name = "sugar", precision = 5, scale = 1)
    private BigDecimal sugar;
}