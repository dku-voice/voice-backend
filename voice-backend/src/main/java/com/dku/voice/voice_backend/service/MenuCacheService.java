package com.dku.voice.voice_backend.service;

import com.dku.voice.voice_backend.dto.MenuResponse;
import com.dku.voice.voice_backend.entity.Menu;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuCacheService {

    private final MenuService menuService;

    /**
     * 전체 활성 메뉴 조회 (캐시만)
     * - Cache Hit  → Redis에서 바로 반환
     * - Cache Miss → MenuService.findAllActive() 호출 후 Redis 저장 (TTL 30분)
     */
    @Cacheable(value = "menus", key = "'all'")
    public List<MenuResponse> getAllMenus() {
        log.info("[MenuCache] Cache Miss - DB 조회");
        return menuService.findAllActive();
    }

    /**
     * 카테고리별 활성 메뉴 조회 (캐시만)
     */
    @Cacheable(value = "menus", key = "'category:' + #categoryId")
    public List<MenuResponse> getMenusByCategory(Long categoryId) {
        log.info("[MenuCache] Cache Miss - DB 조회 (categoryId={})", categoryId);
        return menuService.findActiveByCategoryId(categoryId);
    }

    /**
     * 메뉴 상태 변경 + 전체 캐시 무효화 (관리자용)
     * - 변경된 메뉴 상태가 다음 조회 시 즉시 반영됨
     */
    @CacheEvict(value = "menus", allEntries = true)
    public Menu updateMenuStatus(Long menuId, Menu.MenuStatus status) {
        log.info("[MenuCache] 메뉴 상태 변경 및 캐시 무효화 - menuId={}, status={}", menuId, status);
        return menuService.updateStatus(menuId, status);
    }

    /**
     * 메뉴 캐시 전체 무효화 (긴급 시 사용)
     */
    @CacheEvict(value = "menus", allEntries = true)
    public void evictAllMenuCache() {
        log.info("[MenuCache] 전체 캐시 삭제");
    }
}