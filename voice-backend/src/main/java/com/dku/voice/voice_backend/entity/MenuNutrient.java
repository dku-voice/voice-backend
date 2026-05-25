package com.dku.voice.voice_backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "menu_nutrient")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MenuNutrient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", nullable = false, unique = true)
    private Menu menu;

    @Column(nullable = false)
    private Integer calories;

    @Column(nullable = false)
    private Integer sodium;

    @Column(nullable = false, precision = 5, scale = 1)
    private Double protein;

    @Column(nullable = false, precision = 5, scale = 1)
    private Double fat;

    @Column(nullable = false, precision = 5, scale = 1)
    private Double carbs;
}