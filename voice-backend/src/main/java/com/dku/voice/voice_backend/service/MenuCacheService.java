package com.dku.voice.voice_backend.service;

import com.dku.voice.voice_backend.dto.MenuResponse;
import com.dku.voice.voice_backend.repository.MenuRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuCacheService {

    private final MenuRepository menuRepository;

    /**
     * 서버 시작 시 자동 실행 - Cache Warming
     * - Spring Context 완전히 로드된 후 실행
     * - 키오스크 전원 ON 시 첫 고객부터 Cache Hit 보장
     * - Redis 장애 시 예외를 잡아 서버 시작은 정상 진행 (DB 폴백)
     */
    @PostConstruct
    public void warmup() {
        try {
            getAllMenus();
            log.info("[CacheWarmup] 메뉴 캐시 워밍 완료");
        } catch (Exception e) {
            log.error("[CacheWarmup] 캐시 워밍 실패 - DB 직접 조회로 폴백됩니다. error={}", e.getMessage());
        }
    }

    /**
     * 전체 활성 메뉴 조회 (캐싱 적용)
     * - Cache Hit  → Redis에서 바로 반환 (DB 조회 없음)
     * - Cache Miss → DB 조회 후 Redis 저장 (TTL 30분)
     * - 캐시 키: "menus::all"
     */
    @Cacheable(value = "menus", key = "'all'")
    @Transactional(readOnly = true)
    public List<MenuResponse> getAllMenus() {
        log.info("[MenuCache] Cache Miss - DB 조회");
        return menuRepository.findAllActiveWithDetails().stream()
                .map(MenuResponse::from)
                .toList();
    }

    /**
     * 카테고리별 활성 메뉴 조회 (캐싱 적용)
     * - 캐시 키: "menus::category:{categoryId}"
     */
    @Cacheable(value = "menus", key = "'category:' + #categoryId")
    @Transactional(readOnly = true)
    public List<MenuResponse> getMenusByCategory(Long categoryId) {
        log.info("[MenuCache] Cache Miss - DB 조회 (categoryId={})", categoryId);
        return menuRepository.findActiveByCategoryId(categoryId).stream()
                .map(MenuResponse::from)
                .toList();
    }

    /**
     * 메뉴 캐시 전체 무효화
     * - 추후 관리자 메뉴 상태 변경 API 구현 시 호출
     * - 품절(SOLD_OUT) / 미판매(INACTIVE) 처리 시 사용
     */
    @CacheEvict(value = "menus", allEntries = true)
    public void evictAllMenuCache() {
        log.info("[MenuCache] 전체 캐시 삭제");
    }
}