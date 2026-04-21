package com.dku.voice.voice_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(name = "allergens")
public class Allergen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "name_ko", nullable = false, length = 30)
    private String nameKo;

    @Column(name = "name_en", nullable = false, length = 30)
    private String nameEn;
}