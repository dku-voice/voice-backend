package com.dku.voice.voice_backend.dto;

import java.util.List;

/**
 * AI 서버용 메뉴 응답.
 * 프론트용 MenuResponse(Redis 캐시 대상)와 분리해서 프론트 응답 형태를 바꾸지 않는다.
 * 별칭·태그 같은 음성 인식용 데이터는 AI 서버가 menuId 기준으로 직접 관리한다.
 * 다국어(ja/es/th) 이름은 다국어 음성 인식에서 메뉴명을 매칭하는 데 쓴다.
 */
public record AiMenuResponse(List<MenuDoc> menus) {

    public record MenuDoc(
            Long menuId,
            String nameKo,
            String nameEn,
            String nameJa,
            String nameEs,
            String nameTh,
            String descriptionKo,
            String descriptionEn,
            String categoryKo,
            String categoryEn,
            String categoryJa,
            String categoryEs,
            String categoryTh,
            Integer price,
            String status,
            List<String> allergensKo,
            List<String> allergensJa,
            List<String> allergensEs,
            List<String> allergensTh,
            NutrientDoc nutrient,
            List<OptionDoc> options
    ) {}

    public record NutrientDoc(
            Integer calories,
            Integer sodium,
            Double protein,
            Double fat,
            Double carbs
    ) {}

    public record OptionDoc(
            Long optionId,
            String nameKo,
            String nameEn,
            String nameJa,
            String nameEs,
            String nameTh,
            String optionType,   // "ADD" | "REMOVE"
            Integer extraPrice
    ) {}
}