package com.dku.voice.voice_backend.controller;

import com.dku.voice.voice_backend.dto.MenuResponse;
import com.dku.voice.voice_backend.service.MenuService;
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

    private final MenuService menuService;

    /**
     * GET /api/menus
     * GET /api/menus?categoryId=1   (카테고리 필터)
     *
     * 응답 예시:
     * [
     *   {
     *     "id": 1,
     *     "categoryId": 1,
     *     "categoryNameKo": "버거",
     *     "categoryNameEn": "Burger",
     *     "nameKo": "클래식 버거",
     *     "nameEn": "Classic Burger",
     *     "descriptionKo": "100% 순쇠고기 패티...",
     *     "descriptionEn": "Juicy beef patty...",
     *     "price": 6500,
     *     "imageUrl": null,
     *     "isLimited": false,
     *     "stock": null,
     *     "sortOrder": 1,
     *     "allergens": [
     *       { "code": "GLUTEN", "nameKo": "밀(글루텐)", "nameEn": "Gluten" },
     *       ...
     *     ],
     *     "nutrient": {
     *       "calories": 520,
     *       "protein": 28.0,
     *       ...
     *     }
     *   }
     * ]
     */
    @GetMapping
    public ResponseEntity<List<MenuResponse>> getMenus(
            @RequestParam(required = false) Long categoryId
    ) {
        List<MenuResponse> menus = (categoryId != null)
                ? menuService.getMenusByCategory(categoryId)
                : menuService.getAllMenus();

        return ResponseEntity.ok(menus);
    }
}