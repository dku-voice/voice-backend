package com.dku.voice.voice_backend.controller;

import com.dku.voice.voice_backend.dto.ApiResponse;
import com.dku.voice.voice_backend.dto.AiMenuResponse;
import com.dku.voice.voice_backend.service.AiMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiMenuController {

    private final AiMenuService aiMenuService;

    /**
     * GET /api/ai/menus
     * AI 서버가 시작할 때와 메뉴 변경 알림(POST {AI서버}/api/menus/reload)을 받았을 때 호출한다.
     * 메뉴·옵션 ID는 백엔드 ID 그대로이므로, AI가 이 ID를 주문 결과에 그대로 쓰면
     * 프론트가 /api/orders에 변환 없이 전달할 수 있다.
     * 프론트에서 호출하는 API가 아니다.
     */
    @GetMapping("/menus")
    public ResponseEntity<ApiResponse<AiMenuResponse>> getMenus() {
        return ResponseEntity.ok(ApiResponse.success(aiMenuService.getMenusForAi()));
    }
}