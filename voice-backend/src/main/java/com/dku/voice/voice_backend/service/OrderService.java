package com.dku.voice.voice_backend.service;

import com.dku.voice.voice_backend.dto.OrderItemRequest;
import com.dku.voice.voice_backend.dto.OrderRequest;
import com.dku.voice.voice_backend.dto.OrderResponse;
import com.dku.voice.voice_backend.entity.Menu;
import com.dku.voice.voice_backend.entity.MenuOption;
import com.dku.voice.voice_backend.entity.Order;
import com.dku.voice.voice_backend.entity.OrderItem;
import com.dku.voice.voice_backend.entity.OrderItemOption;
import com.dku.voice.voice_backend.repository.MenuOptionRepository;
import com.dku.voice.voice_backend.repository.MenuRepository;
import com.dku.voice.voice_backend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final MenuRepository menuRepository;
    private final MenuOptionRepository menuOptionRepository;

    /**
     * 주문 생성
     * 1. 메뉴 조회 및 상태 검증 (ACTIVE만 허용)
     * 2. 옵션 조회 및 검증 (해당 메뉴의 옵션인지 확인)
     * 3. 단가 계산: 메뉴 가격 + 선택 옵션 추가금액 합계
     * 4. 총금액 계산 후 Order 생성 (서버 계산 → 위변조 방지)
     * 5. Order + OrderItem + OrderItemOption 저장
     */
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {

        // 1~3. 아이템별 메뉴/옵션 검증 및 단가 계산
        record ItemData(Menu menu, List<MenuOption> options, int unitPrice, int quantity) {}

        List<ItemData> itemDataList = request.getItems().stream()
                .map(itemRequest -> {
                    // 메뉴 조회 및 상태 검증
                    Menu menu = menuRepository.findById(itemRequest.getMenuId())
                            .filter(m -> m.getStatus() == Menu.MenuStatus.ACTIVE)
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "주문할 수 없는 메뉴입니다. menuId=" + itemRequest.getMenuId()));

                    // 옵션 조회 및 해당 메뉴 소속 여부 검증
                    List<MenuOption> selectedOptions = menuOptionRepository
                            .findAllById(itemRequest.getOptionIds())
                            .stream()
                            .peek(option -> {
                                if (!option.getMenu().getId().equals(menu.getId())) {
                                    throw new IllegalArgumentException(
                                            "메뉴에 존재하지 않는 옵션입니다. optionId=" + option.getId());
                                }
                            })
                            .toList();

                    // 단가 = 메뉴 가격 + 옵션 추가금액 합계
                    int optionExtraPrice = selectedOptions.stream()
                            .mapToInt(MenuOption::getExtraPrice)
                            .sum();

                    return new ItemData(menu, selectedOptions,
                            menu.getPrice() + optionExtraPrice,
                            itemRequest.getQuantity());
                })
                .toList();

        // 4. 총금액 서버에서 계산 (클라이언트 금액 신뢰 안 함)
        int totalPrice = itemDataList.stream()
                .mapToInt(d -> d.unitPrice() * d.quantity())
                .sum();

        // 5. Order 생성
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .totalPrice(totalPrice)
                .build();

        // 6. OrderItem + OrderItemOption 생성 및 연결
        itemDataList.forEach(data -> {
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .menu(data.menu())
                    .quantity(data.quantity())
                    .unitPrice(data.unitPrice())
                    .build();

            data.options().stream()
                    .map(option -> OrderItemOption.builder()
                            .orderItem(orderItem)
                            .menuOption(option)
                            .build())
                    .forEach(orderItem.getOptions()::add);

            order.getOrderItems().add(orderItem);
        });

        orderRepository.save(order);

        return OrderResponse.from(order);
    }

    /**
     * 영수증 조회 - 주문 ID로 단건 조회
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "주문을 찾을 수 없습니다. orderId=" + orderId));
        return OrderResponse.from(order);
    }

    /**
     * 주문번호 생성: ORD-yyyyMMdd-UUID 앞 8자리
     */
    private String generateOrderNumber() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "ORD-" + date + "-" + uuid;
    }
}