package com.dku.voice.voice_backend.controller;

import com.dku.voice.voice_backend.dto.ApiResponse;
import com.dku.voice.voice_backend.dto.MenuResponse;
import com.dku.voice.voice_backend.entity.Menu;
import com.dku.voice.voice_backend.service.MenuCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/admin/menus")
@RequiredArgsConstructor
public class AdminMenuController {

    private final MenuCacheService menuCacheService;

    /**
     * PATCH /admin/menus/{id}/status
     * 메뉴 상태 변경
     * - ACTIVE   : 정상 판매
     * - SOLD_OUT : 품절 (재입고 가능)
     * - INACTIVE : 비활성 (메뉴 목록 제외)
     * - 변경 후 Redis 캐시 자동 무효화 (MenuCacheService.updateMenuStatus)
     */
    @PatchMapping("/{menuId}/status")
    public ResponseEntity<ApiResponse<Void>> updateMenuStatus(
            @PathVariable Long menuId,
            @RequestParam Menu.MenuStatus status) {

        menuCacheService.updateMenuStatus(menuId, status);
        log.info("[Admin] 메뉴 상태 변경 - menuId={}, status={}", menuId, status);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}