package com.dku.voice.voice_backend.dto;

import com.dku.voice.voice_backend.entity.Allergen;
import com.dku.voice.voice_backend.entity.Menu;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.function.Function;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuResponse {

    private Long menuId;
    private String nameKo;
    private String nameEn;
    private String nameJa;
    private String nameEs;
    private String nameTh;
    private Integer price;
    private String imageUrl;
    private String status;
    private String categoryNameKo;
    private String categoryNameEn;
    private String categoryNameJa;
    private String categoryNameEs;
    private String categoryNameTh;
    private List<String> allergensKo;
    private List<String> allergensEn;
    private List<String> allergensJa;
    private List<String> allergensEs;
    private List<String> allergensTh;
    private NutrientInfo nutrient;
    private List<MenuOptionInfo> options;

    public static MenuResponse from(Menu menu) {
        return MenuResponse.builder()
                .menuId(menu.getId())
                .nameKo(menu.getNameKo())
                .nameEn(menu.getNameEn())
                .nameJa(I18n.or(menu.getNameJa(), menu.getNameEn()))
                .nameEs(I18n.or(menu.getNameEs(), menu.getNameEn()))
                .nameTh(I18n.or(menu.getNameTh(), menu.getNameEn()))
                .price(menu.getPrice())
                .imageUrl(menu.getImageUrl())
                .status(menu.getStatus().name())
                .categoryNameKo(menu.getCategory().getNameKo())
                .categoryNameEn(menu.getCategory().getNameEn())
                .categoryNameJa(I18n.or(menu.getCategory().getNameJa(), menu.getCategory().getNameEn()))
                .categoryNameEs(I18n.or(menu.getCategory().getNameEs(), menu.getCategory().getNameEn()))
                .categoryNameTh(I18n.or(menu.getCategory().getNameTh(), menu.getCategory().getNameEn()))
                .allergensKo(allergens(menu, Allergen::getNameKo))
                .allergensEn(allergens(menu, Allergen::getNameEn))
                .allergensJa(allergens(menu, a -> I18n.or(a.getNameJa(), a.getNameEn())))
                .allergensEs(allergens(menu, a -> I18n.or(a.getNameEs(), a.getNameEn())))
                .allergensTh(allergens(menu, a -> I18n.or(a.getNameTh(), a.getNameEn())))
                .nutrient(menu.getMenuNutrient() != null
                        ? NutrientInfo.from(menu.getMenuNutrient())
                        : null)
                .options(menu.getMenuOptions().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                com.dku.voice.voice_backend.entity.MenuOption::getId,
                                o -> o,
                                (a, b) -> a,
                                java.util.TreeMap::new))      // id 오름차순 + 중복 제거
                        .values().stream()
                        .map(MenuOptionInfo::from)
                        .toList())
                .build();
    }

    private static List<String> allergens(Menu menu, Function<Allergen, String> name) {
        return menu.getMenuAllergens().stream()
                .map(ma -> name.apply(ma.getAllergen()))
                .toList();
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
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
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MenuOptionInfo {
        private Long optionId;
        private String nameKo;
        private String nameEn;
        private String nameJa;
        private String nameEs;
        private String nameTh;
        private String optionType;   // "ADD" | "REMOVE"
        private Integer extraPrice;

        public static MenuOptionInfo from(com.dku.voice.voice_backend.entity.MenuOption option) {
            return MenuOptionInfo.builder()
                    .optionId(option.getId())
                    .nameKo(option.getNameKo())
                    .nameEn(option.getNameEn())
                    .nameJa(I18n.or(option.getNameJa(), option.getNameEn()))
                    .nameEs(I18n.or(option.getNameEs(), option.getNameEn()))
                    .nameTh(I18n.or(option.getNameTh(), option.getNameEn()))
                    .optionType(option.getOptionType())
                    .extraPrice(option.getExtraPrice())
                    .build();
        }
    }
}