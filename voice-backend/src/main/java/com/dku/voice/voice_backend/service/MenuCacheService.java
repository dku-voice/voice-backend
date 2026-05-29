package com.dku.voice.voice_backend.service;

import com.dku.voice.voice_backend.dto.MenuResponse;
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
     * - Cache Hit  → Redis에서 바로 반환 (DB 조회 없음)
     * - Cache Miss → MenuService.findAllActive() 호출 후 Redis 저장 (TTL 30분)
     * - 캐시 키: "menus::all"
     */
    @Cacheable(value = "menus", key = "'all'")
    public List<MenuResponse> getAllMenus() {
        log.info("[MenuCache] Cache Miss - DB 조회");
        return menuService.findAllActive()
                .stream()
                .map(MenuResponse::from)
                .toList();
    }

    /**
     * 카테고리별 활성 메뉴 조회 (캐시만)
     * - 캐시 키: "menus::category:{categoryId}"
     */
    @Cacheable(value = "menus", key = "'category:' + #categoryId")
    public List<MenuResponse> getMenusByCategory(Long categoryId) {
        log.info("[MenuCache] Cache Miss - DB 조회 (categoryId={})", categoryId);
        return menuService.findActiveByCategoryId(categoryId)
                .stream()
                .map(MenuResponse::from)
                .toList();
    }

    /**
     * 메뉴 캐시 전체 무효화
     * - 추후 관리자 메뉴 상태 변경 API 구현 시 호출
     */
    @CacheEvict(value = "menus", allEntries = true)
    public void evictAllMenuCache() {
        log.info("[MenuCache] 전체 캐시 삭제");
    }
}