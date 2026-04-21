package com.dku.voice.voice_backend.service;

import com.dku.voice.voice_backend.dto.MenuResponse;
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
     * 전체 메뉴 목록 조회 (활성화된 메뉴만).
     * readOnly=true → MySQL Master-Slave 환경에서 Slave DB로 라우팅됨.
     */
    @Transactional(readOnly = true)
    public List<MenuResponse> getAllMenus() {
        return menuRepository.findAllActiveWithDetails()
                .stream()
                .map(MenuResponse::from)
                .toList();
    }

    /**
     * 카테고리별 메뉴 조회.
     */
    @Transactional(readOnly = true)
    public List<MenuResponse> getMenusByCategory(Long categoryId) {
        return menuRepository.findActiveByCategoryId(categoryId)
                .stream()
                .map(MenuResponse::from)
                .toList();
    }
}