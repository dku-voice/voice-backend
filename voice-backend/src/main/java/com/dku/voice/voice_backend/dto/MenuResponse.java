package com.dku.voice.voice_backend.dto;

import com.dku.voice.voice_backend.entity.Menu;
import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class MenuResponse {

    private Long menuId;
    private String nameKo;
    private String nameEn;
    private Integer price;
    private String imageUrl;
    private String status;
    private String categoryNameKo;
    private String categoryNameEn;
    private List<String> allergensKo;
    private List<String> allergensEn;
    private NutrientInfo nutrient;
    private List<MenuOptionInfo> options;

    public static MenuResponse from(Menu menu) {
        return MenuResponse.builder()
                .menuId(menu.getId())
                .nameKo(menu.getNameKo())
                .nameEn(menu.getNameEn())
                .price(menu.getPrice())
                .imageUrl(menu.getImageUrl())
                .status(menu.getStatus().name())
                .categoryNameKo(menu.getCategory().getNameKo())
                .categoryNameEn(menu.getCategory().getNameEn())
                .allergensKo(menu.getMenuAllergens().stream()
                        .map(ma -> ma.getAllergen().getNameKo())
                        .toList())
                .allergensEn(menu.getMenuAllergens().stream()
                        .map(ma -> ma.getAllergen().getNameEn())
                        .toList())
                .nutrient(menu.getMenuNutrient() != null
                        ? NutrientInfo.from(menu.getMenuNutrient())
                        : null)
                .options(menu.getMenuOptions().stream()
                        .map(MenuOptionInfo::from)
                        .toList())
                .build();
    }

    @Getter
    @Builder
    public static class NutrientInfo {
        private Integer calories;
        private Integer sodium;
        private Double protein;
        private Double fat;
        private Double carbs;

        public static NutrientInfo from(com.dku.voice.voice_backend.entity.MenuNutrient nutrient) {
            return NutrientInfo.builder()
                    .calories(nutrient.getCalories())
                    .sodium(nutrient.getSodium())
                    .protein(nutrient.getProtein())
                    .fat(nutrient.getFat())
                    .carbs(nutrient.getCarbs())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class MenuOptionInfo {
        private Long optionId;
        private String nameKo;
        private String nameEn;
        private String optionType;   // "ADD" | "REMOVE"
        private Integer extraPrice;

        public static MenuOptionInfo from(com.dku.voice.voice_backend.entity.MenuOption option) {
            return MenuOptionInfo.builder()
                    .optionId(option.getId())
                    .nameKo(option.getNameKo())
                    .nameEn(option.getNameEn())
                    .optionType(option.getOptionType())
                    .extraPrice(option.getExtraPrice())
                    .build();
        }
    }
}