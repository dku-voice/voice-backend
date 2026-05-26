package com.dku.voice.voice_backend.controller;

import com.dku.voice.voice_backend.dto.ApiResponse;
import com.dku.voice.voice_backend.dto.MenuResponse;
import com.dku.voice.voice_backend.service.MenuCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuCacheService menuCacheService;

    /**
     * 전체 활성 메뉴 조회
     * GET /api/menus
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<MenuResponse>>> getMenus(
            @RequestParam(required = false) Long categoryId) {

        List<MenuResponse> menus = (categoryId != null)
                ? menuCacheService.getMenusByCategory(categoryId)
                : menuCacheService.getAllMenus();

        return ResponseEntity.ok(ApiResponse.success(menus));
    }
}