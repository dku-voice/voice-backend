package com.dku.voice.voice_backend.service;

import com.dku.voice.voice_backend.dto.MenuResponse;
import com.dku.voice.voice_backend.entity.Menu;
import com.dku.voice.voice_backend.event.MenuStatusChangedEvent;
import com.dku.voice.voice_backend.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 전체 활성 메뉴 조회 (트랜잭션만)
     * - 트랜잭션 안에서 DTO 변환까지 완료
     * - ArrayList 반환 → activateDefaultTyping과 호환
     */
    @Transactional(readOnly = true)
    public List<MenuResponse> findAllActive() {
        return menuRepository.findAllActiveWithDetails()
                .stream()
                .map(MenuResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 카테고리별 활성 메뉴 조회 (트랜잭션만)
     */
    @Transactional(readOnly = true)
    public List<MenuResponse> findActiveByCategoryId(Long categoryId) {
        return menuRepository.findActiveByCategoryId(categoryId)
                .stream()
                .map(MenuResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 메뉴 상태 변경 (관리자용)
     * - DB 변경 후 MenuStatusChangedEvent 발행
     * - 트랜잭션 커밋 완료 후 MenuCacheService에서 캐시 삭제
     */
    @Transactional
    public Menu updateStatus(Long menuId, Menu.MenuStatus status) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "메뉴를 찾을 수 없습니다. menuId=" + menuId));
        menu.changeStatus(status);

        // 트랜잭션 커밋 후 캐시 삭제를 위한 이벤트 발행
        eventPublisher.publishEvent(new MenuStatusChangedEvent(menuId));

        return menu;
    }
}