package com.dku.voice.voice_backend.config;

import com.dku.voice.voice_backend.entity.Category;
import com.dku.voice.voice_backend.entity.Menu;
import com.dku.voice.voice_backend.repository.CategoryRepository;
import com.dku.voice.voice_backend.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final CategoryRepository categoryRepository;
    private final MenuRepository menuRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        // 이미 데이터가 있으면 스킵 (중복 방지)
        if (categoryRepository.count() > 0) return;

        // ── 카테고리 ──────────────────────────────
        Category burger  = categoryRepository.save(Category.builder().nameKo("버거").nameEn("Burger").sortOrder(1).build());
        Category side    = categoryRepository.save(Category.builder().nameKo("사이드").nameEn("Side").sortOrder(2).build());
        Category drink   = categoryRepository.save(Category.builder().nameKo("음료").nameEn("Drink").sortOrder(3).build());
        Category dessert = categoryRepository.save(Category.builder().nameKo("디저트").nameEn("Dessert").sortOrder(4).build());

        // ── 메뉴 ──────────────────────────────────
        // 버거
        menuRepository.save(Menu.builder()
                .category(burger)
                .nameKo("클래식 버거").nameEn("Classic Burger")
                .descriptionKo("100% 순쇠고기 패티와 신선한 채소의 조화")
                .descriptionEn("Juicy beef patty with fresh vegetables")
                .price(6500).sortOrder(1).build());

        menuRepository.save(Menu.builder()
                .category(burger)
                .nameKo("치즈 버거").nameEn("Cheese Burger")
                .descriptionKo("고소한 체다 치즈를 듬뿍 올린 버거")
                .descriptionEn("Classic burger topped with rich cheddar")
                .price(7500).sortOrder(2).build());

        menuRepository.save(Menu.builder()
                .category(burger)
                .nameKo("스파이시 버거").nameEn("Spicy Burger")
                .descriptionKo("매콤한 소스와 할라피뇨가 들어간 버거")
                .descriptionEn("Burger with spicy sauce and jalapeño")
                .price(7000).isLimited(true).stock(50).sortOrder(3).build());

        // 사이드
        menuRepository.save(Menu.builder()
                .category(side)
                .nameKo("감자튀김").nameEn("French Fries")
                .descriptionKo("바삭하게 튀긴 황금빛 감자튀김")
                .descriptionEn("Crispy golden fries")
                .price(2500).sortOrder(1).build());

        menuRepository.save(Menu.builder()
                .category(side)
                .nameKo("어니언링").nameEn("Onion Rings")
                .descriptionKo("달콤한 양파에 바삭한 튀김옷을 입힌 메뉴")
                .descriptionEn("Sweet onions in crispy batter")
                .price(2800).sortOrder(2).build());

        // 음료
        menuRepository.save(Menu.builder()
                .category(drink)
                .nameKo("콜라").nameEn("Cola")
                .descriptionKo("시원하고 청량한 탄산음료")
                .descriptionEn("Refreshing carbonated drink")
                .price(1500).sortOrder(1).build());

        menuRepository.save(Menu.builder()
                .category(drink)
                .nameKo("아메리카노").nameEn("Americano")
                .descriptionKo("진하고 깊은 풍미의 에스프레소 기반 커피")
                .descriptionEn("Rich espresso-based black coffee")
                .price(2500).sortOrder(2).build());

        // 디저트
        menuRepository.save(Menu.builder()
                .category(dessert)
                .nameKo("아이스크림").nameEn("Ice Cream")
                .descriptionKo("부드럽고 달콤한 소프트 아이스크림")
                .descriptionEn("Soft and sweet ice cream")
                .price(1000).sortOrder(1).build());
    }
}