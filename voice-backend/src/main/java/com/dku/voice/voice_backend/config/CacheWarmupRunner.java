package com.dku.voice.voice_backend.config;

import com.dku.voice.voice_backend.service.MenuCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheWarmupRunner implements ApplicationRunner {

    private final MenuCacheService menuCacheService;

    /**
     * 서버 시작 완료 후 자동 실행 - Cache Warming
     * - ApplicationRunner: Spring Context 완전히 로드된 후 실행
     * - MenuCacheService와 별도 클래스 → self-invocation 없음
     * - MenuCacheService.getAllMenus()를 외부에서 호출
     *   → 프록시를 통해 @Cacheable, @Transactional 정상 적용
     */
    @Override
    public void run(ApplicationArguments args) {
        try {
            menuCacheService.getAllMenus();
            log.info("[CacheWarmup] 메뉴 캐시 워밍 완료");
        } catch (Exception e) {
            log.error("[CacheWarmup] 캐시 워밍 실패 - DB 직접 조회로 폴백됩니다. error={}", e.getMessage());
        }
    }
}