package com.dku.voice.voice_backend.service;

import com.dku.voice.voice_backend.dto.AiMenuResponse;
import com.dku.voice.voice_backend.dto.I18n;
import com.dku.voice.voice_backend.entity.Allergen;
import com.dku.voice.voice_backend.entity.Menu;
import com.dku.voice.voice_backend.entity.MenuNutrient;
import com.dku.voice.voice_backend.entity.MenuOption;
import com.dku.voice.voice_backend.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * AI 서버가 가져갈 메뉴 데이터를 만든다.
 * - INACTIVE 메뉴는 제외, ACTIVE/SOLD_OUT은 status와 함께 포함 (품절 안내용)
 * - 캐시를 거치지 않고 DB에서 직접 읽는다 (AI 서버 시작/재동기화 때만 호출됨)
 * - 연관 엔티티를 지연 로딩으로 읽으므로 반드시 트랜잭션 안에서 DTO 변환까지 끝낸다
 * - 번역(ja/es/th)이 없으면 영어 이름으로 대체한다
 */
@Service
@RequiredArgsConstructor
public class AiMenuService {

    private final MenuRepository menuRepository;

    @Transactional(readOnly = true)
    public AiMenuResponse getMenusForAi() {
        List<AiMenuResponse.MenuDoc> menus = menuRepository.findAll().stream()
                .filter(m -> m.getStatus() != Menu.MenuStatus.INACTIVE)
                .sorted(Comparator.comparing(Menu::getId))
                .map(this::toDoc)
                .toList();
        return new AiMenuResponse(menus);
    }

    private AiMenuResponse.MenuDoc toDoc(Menu m) {
        MenuNutrient n = m.getMenuNutrient();
        var c = m.getCategory();

        return new AiMenuResponse.MenuDoc(
                m.getId(),
                m.getNameKo(),
                m.getNameEn(),
                I18n.or(m.getNameJa(), m.getNameEn()),
                I18n.or(m.getNameEs(), m.getNameEn()),
                I18n.or(m.getNameTh(), m.getNameEn()),
                m.getDescriptionKo(),
                m.getDescriptionEn(),
                c.getNameKo(),
                c.getNameEn(),
                I18n.or(c.getNameJa(), c.getNameEn()),
                I18n.or(c.getNameEs(), c.getNameEn()),
                I18n.or(c.getNameTh(), c.getNameEn()),
                m.getPrice(),
                m.getStatus().name(),
                allergens(m, Allergen::getNameKo),
                allergens(m, a -> I18n.or(a.getNameJa(), a.getNameEn())),
                allergens(m, a -> I18n.or(a.getNameEs(), a.getNameEn())),
                allergens(m, a -> I18n.or(a.getNameTh(), a.getNameEn())),
                n == null ? null : new AiMenuResponse.NutrientDoc(
                        n.getCalories(), n.getSodium(), n.getProtein(), n.getFat(), n.getCarbs()),
                m.getMenuOptions().stream()
                        .sorted(Comparator.comparing(MenuOption::getId))
                        .map(o -> new AiMenuResponse.OptionDoc(
                                o.getId(), o.getNameKo(), o.getNameEn(),
                                I18n.or(o.getNameJa(), o.getNameEn()),
                                I18n.or(o.getNameEs(), o.getNameEn()),
                                I18n.or(o.getNameTh(), o.getNameEn()),
                                o.getOptionType(), o.getExtraPrice()))
                        .toList()
        );
    }

    private List<String> allergens(Menu m, Function<Allergen, String> name) {
        return m.getMenuAllergens().stream()
                .map(ma -> name.apply(ma.getAllergen()))
                .toList();
    }
}