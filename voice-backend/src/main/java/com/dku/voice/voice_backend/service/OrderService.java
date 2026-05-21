package com.dku.voice.voice_backend.service;

import com.dku.voice.voice_backend.dto.OrderRequest;
import com.dku.voice.voice_backend.dto.OrderResponse;
import com.dku.voice.voice_backend.entity.Menu;
import com.dku.voice.voice_backend.entity.Order;
import com.dku.voice.voice_backend.entity.OrderItem;
import com.dku.voice.voice_backend.repository.MenuRepository;
import com.dku.voice.voice_backend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final MenuRepository menuRepository;

    /**
     * 주문 생성
     * 1. 요청된 메뉴 ID로 메뉴 조회 (비활성 메뉴 포함 여부 검증)
     * 2. 총금액 계산 (서버에서 직접 계산 → 위변조 방지)
     * 3. Order + OrderItem 저장
     */
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
 
        // 1. 메뉴 조회 및 상태 검증
        // ACTIVE만 허용 - SOLD_OUT, INACTIVE는 주문 불가
        // 프론트에서 1차 차단, 여기서 화면 갱신 안 된 엣지 케이스 방어
        List<OrderItem> orderItems = request.getItems().stream()
                .map(itemRequest -> {
                    Menu menu = menuRepository.findById(itemRequest.getMenuId())
                            .filter(m -> m.getStatus() == Menu.MenuStatus.ACTIVE)
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "주문할 수 없는 메뉴입니다. menuId=" + itemRequest.getMenuId()));
 
                    return OrderItem.builder()
                            .menu(menu)
                            .quantity(itemRequest.getQuantity())
                            .unitPrice(menu.getPrice())
                            .build();
                })
                .toList();


        // 2. 총금액 서버에서 계산 (클라이언트 금액 신뢰 안 함)
        int totalPrice = orderItems.stream()
                .mapToInt(item -> item.getUnitPrice() * item.getQuantity())
                .sum();

        // 3. 주문 생성
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .totalPrice(totalPrice)
                .build();

        // 4. 아이템과 주문 연결 후 저장
        orderItems.forEach(item -> {
            OrderItem linked = OrderItem.builder()
                    .order(order)
                    .menu(item.getMenu())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .build();
            order.getOrderItems().add(linked);
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
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. orderId=" + orderId));
        return OrderResponse.from(order);
    }

    /**
     * 주문번호 생성: ORD-2026xxxx-UUID 앞 8자리
     */
    private String generateOrderNumber() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "ORD-" + date + "-" + uuid;
    }
}