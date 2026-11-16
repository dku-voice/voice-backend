package com.dku.voice.voice_backend.service;

import com.dku.voice.voice_backend.dto.MenuResponse;
import com.dku.voice.voice_backend.entity.Menu;
import com.dku.voice.voice_backend.event.MenuStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

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
     * 메뉴 상태 변경 (관리자용)
     * - MenuService.updateStatus()에서 이벤트 발행
     * - 트랜잭션 커밋 완료 후 onMenuStatusChanged()에서 Write-Through
     */
    public Menu updateMenuStatus(Long menuId, Menu.MenuStatus status) {
        log.info("[MenuCache] 메뉴 상태 변경 요청 - menuId={}, status={}", menuId, status);
        return menuService.updateStatus(menuId, status);
    }

    /**
     * Write-Through - 트랜잭션 커밋 완료 후 캐시 삭제 후 즉시 최신 데이터로 채움
     * - AFTER_COMMIT: 커밋 완료 후 실행 보장 → 롤백 시 캐시 그대로 유지
     * - evictAllMenuCache(): 기존 캐시 삭제
     * - getAllMenus(): DB에서 최신 데이터로 즉시 채움
     * - 캐시가 비어있는 시간 최소화, 항상 최신 데이터 유지
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMenuStatusChanged(MenuStatusChangedEvent event) {
        log.info("[MenuCache] 커밋 완료 - Write-Through 캐시 업데이트 - menuId={}", event.getMenuId());
        evictAllMenuCache();  // 1. 기존 캐시 삭제
        getAllMenus();         // 2. DB에서 최신 데이터로 즉시 채움
    }

    /**
     * 메뉴 캐시 전체 무효화 (긴급 시 사용)
     */
    @CacheEvict(value = "menus", allEntries = true)
    public void evictAllMenuCache() {
        log.info("[MenuCache] 전체 캐시 삭제");
    }
}