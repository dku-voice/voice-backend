package com.dku.voice.voice_backend;

import com.dku.voice.voice_backend.dto.*;
import com.dku.voice.voice_backend.entity.*;
import com.dku.voice.voice_backend.entity.Order;
import com.dku.voice.voice_backend.repository.*;
import com.dku.voice.voice_backend.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("default")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PaymentIntegrationTest {

    @Autowired PaymentService paymentService;
    @Autowired OrderService orderService;
    @Autowired OrderRepository orderRepository;
    @Autowired PaymentRepository paymentRepository;
    @Autowired MenuRepository menuRepository;
    @Autowired MenuOptionRepository menuOptionRepository;
    @Autowired CacheManager cacheManager;
    @Autowired MenuCacheService menuCacheService;
    @Autowired SseEmitterService sseEmitterService;
    @Autowired MenuService menuService;

    private static Long testOrderId;

    @BeforeEach
    void setUp() {
        OrderRequest orderRequest = OrderRequest.builder()
                .items(List.of(
                        OrderItemRequest.builder()
                                .menuId(1L)
                                .quantity(1)
                                .optionIds(List.of())
                                .build()
                ))
                .build();

        OrderResponse orderResponse = orderService.createOrder(orderRequest);
        testOrderId = orderResponse.getOrderId();
    }

    @AfterEach
    void tearDown() {
        paymentRepository.findByOrderId(testOrderId)
                .ifPresent(paymentRepository::delete);
        orderRepository.findById(testOrderId)
                .ifPresent(orderRepository::delete);
    }

    // ════════════════════════════════════════════════════════════════════════
    // 1. 결제 위변조 검증
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @org.junit.jupiter.api.Order(1)
    @DisplayName("[통합] 위변조 검증 - 실제 DB 주문 금액과 다른 금액 요청 시 예외 발생")
    void integration_amountTampering_throwsException() {
        PaymentRequest request = PaymentRequest.builder()
                .pgProvider("TEST")
                .pgTransactionId("tgen-tamper-001")
                .orderId(testOrderId)
                .amount(1000)
                .method("CARD")
                .build();

        assertThatThrownBy(() -> paymentService.confirmPayment(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("결제 금액이 주문 금액과 다릅니다");

        assertThat(paymentRepository.findByPgTransactionId("tgen-tamper-001")).isEmpty();

        Order order = orderRepository.findById(testOrderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.PENDING);
    }

    // ════════════════════════════════════════════════════════════════════════
    // 2. 트랜잭션 롤백
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @org.junit.jupiter.api.Order(2)
    @DisplayName("[통합] 트랜잭션 롤백 - 이미 결제된 주문 재결제 시 Order 상태 변경 없음")
    void integration_rollback_alreadyPaidOrder() {
        PaymentRequest firstRequest = PaymentRequest.builder()
                .pgProvider("TEST")
                .pgTransactionId("tgen-rollback-001")
                .orderId(testOrderId)
                .amount(2800)
                .method("CARD")
                .build();
        paymentService.confirmPayment(firstRequest);

        Order order = orderRepository.findById(testOrderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.PAID);

        PaymentRequest secondRequest = PaymentRequest.builder()
                .pgProvider("TEST")
                .pgTransactionId("tgen-rollback-002")
                .orderId(testOrderId)
                .amount(2800)
                .method("CARD")
                .build();

        assertThatThrownBy(() -> paymentService.confirmPayment(secondRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 결제 완료된 주문입니다");

        assertThat(paymentRepository.findByPgTransactionId("tgen-rollback-002")).isEmpty();
    }

    // ════════════════════════════════════════════════════════════════════════
    // 3. 분산 락
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @org.junit.jupiter.api.Order(3)
    @DisplayName("[통합] 분산 락 - 동일 주문 동시 결제 시 1건만 성공")
    void integration_distributedLock_concurrentPayment() throws InterruptedException {
        int threadCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    PaymentRequest request = PaymentRequest.builder()
                            .pgProvider("TEST")
                            .pgTransactionId("tgen-lock-" + index)
                            .orderId(testOrderId)
                            .amount(2800)
                            .method("CARD")
                            .build();
                    paymentService.confirmPayment(request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(threadCount - 1);

        Order order = orderRepository.findById(testOrderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.PAID);
    }

    // ════════════════════════════════════════════════════════════════════════
    // 4. 환불
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @org.junit.jupiter.api.Order(4)
    @DisplayName("[통합] 환불 - 실제 DB에서 PAID → REFUNDED 상태 변경 확인")
    void integration_cancelPayment_success() {
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .pgProvider("TEST")
                .pgTransactionId("tgen-cancel-001")
                .orderId(testOrderId)
                .amount(2800)
                .method("CARD")
                .build();
        paymentService.confirmPayment(paymentRequest);

        Order paidOrder = orderRepository.findById(testOrderId).orElseThrow();
        assertThat(paidOrder.getStatus()).isEqualTo(Order.OrderStatus.PAID);

        PaymentCancelRequest cancelRequest = PaymentCancelRequest.builder()
                .pgTransactionId("tgen-cancel-001")
                .cancelReason("고객 요청")
                .build();
        PaymentCancelResponse response = paymentService.cancelPayment(cancelRequest);

        assertThat(response.getCancelReason()).isEqualTo("고객 요청");
        assertThat(response.getRefundAmount()).isEqualTo(2800);

        Order refundedOrder = orderRepository.findById(testOrderId).orElseThrow();
        assertThat(refundedOrder.getStatus()).isEqualTo(Order.OrderStatus.REFUNDED);
    }

    // ════════════════════════════════════════════════════════════════════════
    // 5. Redis 캐싱
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @org.junit.jupiter.api.Order(5)
    @DisplayName("[통합] Redis 캐싱 - 메뉴 캐시가 실제 Redis에 저장됐는지 확인")
    void integration_redis_menuCacheExists() {
        menuCacheService.getAllMenus();

        var cache = cacheManager.getCache("menus");
        assertThat(cache).isNotNull();

        var cachedMenus = cache.get("all");
        assertThat(cachedMenus).isNotNull();
        assertThat(cachedMenus.get()).isNotNull();
    }

    // ════════════════════════════════════════════════════════════════════════
    // 6. SSE 연결 테스트
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @org.junit.jupiter.api.Order(6)
    @DisplayName("[통합] SSE - storeId 기준으로 KDS 연결 및 해제 확인")
    void integration_sse_connectAndDisconnect() {
        String storeId = "store-test";

        // SSE 연결
        SseEmitter emitter = sseEmitterService.connect(storeId);
        assertThat(emitter).isNotNull();

        // 같은 매장에 여러 연결
        SseEmitter emitter2 = sseEmitterService.connect(storeId);
        assertThat(emitter2).isNotNull();

        // 연결 완료 → 자동 제거
        emitter.complete();
        emitter2.complete();
    }

    @Test
    @org.junit.jupiter.api.Order(7)
    @DisplayName("[통합] SSE - 연결 없는 매장에 이벤트 전송 시 예외 없이 처리")
    void integration_sse_sendToNoConnection() {
        OrderEvent orderEvent = OrderEvent.builder()
                .orderId(999L)
                .orderNumber("ORD-TEST")
                .storeId("store-no-connection")
                .totalPrice(2800)
                .items(List.of())
                .build();

        // 연결된 KDS 없어도 예외 없이 처리되어야 함
        assertThatNoException().isThrownBy(
                () -> sseEmitterService.sendOrderEvent(orderEvent));
    }

    // ════════════════════════════════════════════════════════════════════════
    // 7. KDS 주문 상태 변경
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @org.junit.jupiter.api.Order(8)
    @DisplayName("[통합] KDS - PAID → PREPARING → COMPLETED 상태 전이 확인")
    void integration_kds_statusTransition() {
        // 결제 완료 (PAID)
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .pgProvider("TEST")
                .pgTransactionId("tgen-kds-001")
                .orderId(testOrderId)
                .amount(2800)
                .method("CARD")
                .build();
        paymentService.confirmPayment(paymentRequest);

        Order paidOrder = orderRepository.findById(testOrderId).orElseThrow();
        assertThat(paidOrder.getStatus()).isEqualTo(Order.OrderStatus.PAID);

        // 조리 시작 (PREPARING)
        paidOrder.changeStatus(Order.OrderStatus.PREPARING);
        orderRepository.save(paidOrder);

        Order preparingOrder = orderRepository.findById(testOrderId).orElseThrow();
        assertThat(preparingOrder.getStatus()).isEqualTo(Order.OrderStatus.PREPARING);

        // 조리 완료 (COMPLETED)
        preparingOrder.changeStatus(Order.OrderStatus.COMPLETED);
        orderRepository.save(preparingOrder);

        Order completedOrder = orderRepository.findById(testOrderId).orElseThrow();
        assertThat(completedOrder.getStatus()).isEqualTo(Order.OrderStatus.COMPLETED);
    }

    @Test
    @org.junit.jupiter.api.Order(9)
    @DisplayName("[통합] KDS - PAID/PREPARING 상태 주문만 조회되는지 확인")
    void integration_kds_findActiveOrders() {
        // 결제 완료
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .pgProvider("TEST")
                .pgTransactionId("tgen-kds-002")
                .orderId(testOrderId)
                .amount(2800)
                .method("CARD")
                .build();
        paymentService.confirmPayment(paymentRequest);

        // PAID + PREPARING 상태 주문 조회
        List<Order> activeOrders = orderRepository.findByStoreIdAndStatusIn(
                "store-default",
                List.of(Order.OrderStatus.PAID, Order.OrderStatus.PREPARING)
        );

        assertThat(activeOrders).isNotEmpty();
        assertThat(activeOrders).allMatch(o ->
                o.getStatus() == Order.OrderStatus.PAID ||
                o.getStatus() == Order.OrderStatus.PREPARING
        );
    }

    // ════════════════════════════════════════════════════════════════════════
    // 8. 관리자 - 메뉴 상태 변경
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @org.junit.jupiter.api.Order(10)
    @DisplayName("[통합] 관리자 - 메뉴 상태 SOLD_OUT 변경 및 캐시 무효화 확인")
    void integration_admin_menuStatusChange() {
        // 캐시 워밍
        menuCacheService.getAllMenus();

        // 변경 전 캐시 확인
        var beforeCache = cacheManager.getCache("menus");
        assertThat(beforeCache.get("all")).isNotNull();

        // 메뉴 상태 변경 (SOLD_OUT) + 캐시 무효화
        menuCacheService.updateMenuStatus(1L, Menu.MenuStatus.SOLD_OUT);

        // 캐시 무효화 확인
        var afterCache = cacheManager.getCache("menus");
        assertThat(afterCache.get("all")).isNull();

        // 원복 (ACTIVE)
        menuCacheService.updateMenuStatus(1L, Menu.MenuStatus.ACTIVE);
    }

    // ════════════════════════════════════════════════════════════════════════
    // 9. 관리자 - 주문/결제 조회
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @org.junit.jupiter.api.Order(11)
    @DisplayName("[통합] 관리자 - 매장별 주문 조회 확인")
    void integration_admin_findOrdersByStore() {
        List<Order> orders = orderRepository.findByStoreId("store-default");
        assertThat(orders).isNotEmpty();
    }

    @Test
    @org.junit.jupiter.api.Order(12)
    @DisplayName("[통합] 관리자 - 결제 완료 후 매장별 결제 내역 조회 확인")
    void integration_admin_findPaymentsByStore() {
        // 결제 완료
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .pgProvider("TEST")
                .pgTransactionId("tgen-admin-001")
                .orderId(testOrderId)
                .amount(2800)
                .method("CARD")
                .build();
        paymentService.confirmPayment(paymentRequest);

        // 매장별 결제 내역 조회
        List<com.dku.voice.voice_backend.entity.Payment> payments =
                paymentRepository.findByStoreId("store-default");
        assertThat(payments).isNotEmpty();
    }
}