package com.dku.voice.voice_backend.event;

import com.dku.voice.voice_backend.dto.OrderEvent;

/**
 * 결제 승인(PAID) 트랜잭션 안에서 발행하는 Spring 이벤트.
 * - 엔티티가 아니라 OrderEvent DTO를 담는다 (지연 로딩은 트랜잭션 안에서 DTO 변환 시 끝낸다).
 * - 실제 Kafka 발행은 커밋 이후 OrderEventListener가 한다.
 */
public record OrderPaidEvent(OrderEvent orderEvent) {
}