package com.dku.voice.voice_backend.dto;

import com.dku.voice.voice_backend.entity.Order;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class OrderResponse {

    private Long orderId;
    private String orderNumber;
    private String status;
    private Integer totalPrice;
    private LocalDateTime orderedAt;
    private List<OrderItemResponse> items;

    public static OrderResponse from(Order order) {
        return OrderResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus().name())
                .totalPrice(order.getTotalPrice())
                .orderedAt(order.getOrderedAt())
                .items(order.getOrderItems().stream()
                        .map(OrderItemResponse::from)
                        .toList())
                .build();
    }

    @Getter
    @Builder
    public static class OrderItemResponse {
        private Long menuId;
        private String menuNameKo;
        private String menuNameEn;
        private Integer quantity;
        private Integer unitPrice;
        private Integer subtotal;
        private List<SelectedOptionInfo> options;

        public static OrderItemResponse from(com.dku.voice.voice_backend.entity.OrderItem item) {
            return OrderItemResponse.builder()
                    .menuId(item.getMenu().getId())
                    .menuNameKo(item.getMenu().getNameKo())
                    .menuNameEn(item.getMenu().getNameEn())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .subtotal(item.getUnitPrice() * item.getQuantity())
                    .options(item.getOptions().stream()
                            .map(o -> SelectedOptionInfo.builder()
                                    .optionId(o.getMenuOption().getId())
                                    .nameKo(o.getMenuOption().getNameKo())
                                    .nameEn(o.getMenuOption().getNameEn())
                                    .optionType(o.getMenuOption().getOptionType())
                                    .extraPrice(o.getMenuOption().getExtraPrice())
                                    .build())
                            .toList())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class SelectedOptionInfo {
        private Long optionId;
        private String nameKo;
        private String nameEn;
        private String optionType;
        private Integer extraPrice;
    }
}