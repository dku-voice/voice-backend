package com.dku.voice.voice_backend.service;

import com.dku.voice.voice_backend.entity.Menu;
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
     * 전체 활성 메뉴 조회 (트랜잭션만)
     * - FETCH JOIN으로 연관 데이터 한 번에 로딩
     */
    @Transactional(readOnly = true)
    public List<Menu> findAllActive() {
        return menuRepository.findAllActiveWithDetails();
    }

    /**
     * 카테고리별 활성 메뉴 조회 (트랜잭션만)
     */
    @Transactional(readOnly = true)
    public List<Menu> findActiveByCategoryId(Long categoryId) {
        return menuRepository.findActiveByCategoryId(categoryId);
    }
}