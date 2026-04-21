package com.dku.voice.voice_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.io.Serializable;

// ────────────────────────────────────────────────
// 메뉴 ↔ 알레르기 매핑 (복합 PK)
// ────────────────────────────────────────────────
@Getter
@Entity
@Table(name = "menu_allergens")
@IdClass(MenuAllergen.MenuAllergenId.class)
public class MenuAllergen {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id")
    private Menu menu;

    @Id
    @ManyToOne(fetch = FetchType.EAGER)   // 알레르기명은 항상 함께 노출
    @JoinColumn(name = "allergen_id")
    private Allergen allergen;

    /** 복합 PK 클래스 */
    public static class MenuAllergenId implements Serializable {
        private Long menu;
        private Long allergen;

        public MenuAllergenId() {}
        public MenuAllergenId(Long menu, Long allergen) {
            this.menu = menu;
            this.allergen = allergen;
        }
    }
}