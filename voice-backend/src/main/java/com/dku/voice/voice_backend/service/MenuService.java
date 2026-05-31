package com.dku.voice.voice_backend.service;

import com.dku.voice.voice_backend.entity.Menu;
import com.dku.voice.voice_backend.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;

    /**
     * 전체 활성 메뉴 조회 (트랜잭션만)
     */
    @Transactional(readOnly = true)
    public List<Menu> findAllActive() {
        return menuRepository.findAllActiveWithDetails();
    }

    /**
     * 카테고리별 활성 메뉴 조회 (트랜잭션만)
     */
    @Transactional(readOnly = true)
    public List<Menu> findActiveByCategoryId(Long categoryId) {
        return menuRepository.findActiveByCategoryId(categoryId);
    }

    /**
     * 메뉴 상태 변경 (관리자용)
     * - 변경 후 캐시 무효화는 MenuCacheService.updateMenuStatus()에서 처리
     */
    @Transactional
    public Menu updateStatus(Long menuId, Menu.MenuStatus status) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "메뉴를 찾을 수 없습니다. menuId=" + menuId));
        menu.changeStatus(status);
        return menu;
    }
}