package com.dku.voice.voice_backend.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class MenuStatusChangedEvent {

    /**
     * 메뉴 상태 변경 이벤트
     * - MenuService.updateStatus() 에서 발행
     * - 트랜잭션 커밋 완료 후 MenuCacheService에서 수신
     * - 커밋 완료 후 캐시 삭제 보장
     */
    private final Long menuId;
}