package com.dku.voice.voice_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "menu")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Menu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "name_ko", nullable = false, length = 100)
    private String nameKo;

    @Column(name = "name_en", nullable = false, length = 100)
    private String nameEn;

    @Column(nullable = false)
    private Integer price;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MenuStatus status = MenuStatus.ACTIVE;

    @OneToMany(mappedBy = "menu", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Builder.Default
    private List<MenuAllergen> menuAllergens = new ArrayList<>();

    @OneToOne(mappedBy = "menu", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private MenuNutrient menuNutrient;

    @OneToMany(mappedBy = "menu", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @BatchSize(size = 100)
    @Builder.Default
    private List<MenuOption> menuOptions = new ArrayList<>();

    public enum MenuStatus {
        ACTIVE,    // 정상 판매
        SOLD_OUT,  // 품절 (재입고 가능, 프론트에서 흐리게 표시)
        INACTIVE   // 비활성 (메뉴 목록에서 완전히 제외)
    }
}