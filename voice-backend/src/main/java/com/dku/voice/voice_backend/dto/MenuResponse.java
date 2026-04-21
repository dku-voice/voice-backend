package com.dku.voice.voice_backend.dto;

import com.dku.voice.voice_backend.entity.Menu;
import com.dku.voice.voice_backend.entity.MenuNutrient;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * GET /api/menus 응답 DTO
 * - 다국어(ko/en) 필드 포함
 * - 알레르기, 영양소 중첩 구조
 */
@Getter
@Builder
public class MenuResponse {

    private Long id;
    private Long categoryId;
    private String categoryNameKo;
    private String categoryNameEn;
    private String nameKo;
    private String nameEn;
    private String descriptionKo;
    private String descriptionEn;
    private int price;
    private String imageUrl;
    private boolean isLimited;
    private Integer stock;          // 한정 메뉴만 노출 (null 허용)
    private int sortOrder;
    private List<AllergenInfo> allergens;
    private NutrientInfo nutrient;  // 영양소 없으면 null

    public static MenuResponse from(Menu menu) {
        return MenuResponse.builder()
                .id(menu.getId())
                .categoryId(menu.getCategory().getId())
                .categoryNameKo(menu.getCategory().getNameKo())
                .categoryNameEn(menu.getCategory().getNameEn())
                .nameKo(menu.getNameKo())
                .nameEn(menu.getNameEn())
                .descriptionKo(menu.getDescriptionKo())
                .descriptionEn(menu.getDescriptionEn())
                .price(menu.getPrice())
                .imageUrl(menu.getImageUrl())
                .isLimited(menu.isLimited())
                .stock(menu.isLimited() ? menu.getStock() : null)
                .sortOrder(menu.getSortOrder())
                .allergens(menu.getAllergens().stream()
                        .map(ma -> AllergenInfo.builder()
                                .code(ma.getAllergen().getCode())
                                .nameKo(ma.getAllergen().getNameKo())
                                .nameEn(ma.getAllergen().getNameEn())
                                .build())
                        .toList())
                .nutrient(menu.getNutrient() != null
                        ? NutrientInfo.from(menu.getNutrient())
                        : null)
                .build();
    }

    // ── 중첩 DTO ──────────────────────────────────

    @Getter
    @Builder
    public static class AllergenInfo {
        private String code;
        private String nameKo;
        private String nameEn;
    }

    @Getter
    @Builder
    public static class NutrientInfo {
        private Integer calories;
        private BigDecimal protein;
        private BigDecimal fat;
        private BigDecimal carbs;
        private Integer sodium;
        private BigDecimal sugar;

        public static NutrientInfo from(MenuNutrient n) {
            return NutrientInfo.builder()
                    .calories(n.getCalories())
                    .protein(n.getProtein())
                    .fat(n.getFat())
                    .carbs(n.getCarbs())
                    .sodium(n.getSodium())
                    .sugar(n.getSugar())
                    .build();
        }
    }
}