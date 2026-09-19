package com.dku.voice.voice_backend.dto;

import com.dku.voice.voice_backend.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {

    private Long orderId;
    private String orderNumber;
    private String storeId;
    private Integer totalPrice;
    private LocalDateTime orderedAt;
    private List<OrderItemInfo> items;

    public static OrderEvent from(Order order) {
        return OrderEvent.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .storeId(order.getStoreId())
                .totalPrice(order.getTotalPrice())
                .orderedAt(order.getOrderedAt())
                .items(order.getOrderItems().stream()
                        .map(OrderItemInfo::from)
                        .collect(Collectors.toList()))
                .build();
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemInfo {
        private String menuNameKo;
        private Integer quantity;
        private List<String> optionNames;

        public static OrderItemInfo from(com.dku.voice.voice_backend.entity.OrderItem item) {
            return OrderItemInfo.builder()
                    .menuNameKo(item.getMenu().getNameKo())
                    .quantity(item.getQuantity())
                    .optionNames(item.getOptions().stream()
                            .map(o -> o.getMenuOption().getNameKo())
                            .collect(Collectors.toList()))
                    .build();
        }
    }
}