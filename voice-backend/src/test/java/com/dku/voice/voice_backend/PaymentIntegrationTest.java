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
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * 결제 통합 테스트
 * - 실제 MySQL, Redis(Redisson) 사용
 * - Docker(voice-mysql, voice-redis)가 실행 중이어야 함
 * - 토스 API 호출은 pgProvider="TEST"로 우회
 */
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

    // 테스트용 주문 ID (각 테스트에서 공유)
    private static Long testOrderId;

    // ── 테스트 데이터 준비 ──────────────────────────────────────────────────

    @BeforeEach
    void setUp() {
        // V2 샘플 데이터의 첫 번째 메뉴(데리버거, id=1, price=2800)로 주문 생성
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
        // 테스트 후 생성된 결제/주문 데이터 정리
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
        // given
        // 데리버거 1개 = 2800원, 위변조 금액 = 1000원
        PaymentRequest request = PaymentRequest.builder()
                .pgProvider("TEST")
                .pgTransactionId("tgen-tamper-001")
                .orderId(testOrderId)
                .amount(1000)  // 실제 주문금액 2800과 다름
                .method("CARD")
                .build();

        // when & then
        assertThatThrownBy(() -> paymentService.confirmPayment(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("결제 금액이 주문 금액과 다릅니다");

        // DB에 Payment가 저장되지 않았는지 확인
        assertThat(paymentRepository.findByPgTransactionId("tgen-tamper-001"))
                .isEmpty();

        // Order 상태가 PENDING 유지인지 확인
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
        // given - 먼저 정상 결제
        PaymentRequest firstRequest = PaymentRequest.builder()
                .pgProvider("TEST")
                .pgTransactionId("tgen-rollback-001")
                .orderId(testOrderId)
                .amount(2800)
                .method("CARD")
                .build();
        paymentService.confirmPayment(firstRequest);

        // Order가 PAID 상태인지 확인
        Order order = orderRepository.findById(testOrderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.PAID);

        // when - 같은 주문 재결제 시도
        PaymentRequest secondRequest = PaymentRequest.builder()
                .pgProvider("TEST")
                .pgTransactionId("tgen-rollback-002")
                .orderId(testOrderId)
                .amount(2800)
                .method("CARD")
                .build();

        // then - 예외 발생
        assertThatThrownBy(() -> paymentService.confirmPayment(secondRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 결제 완료된 주문입니다");

        // 두 번째 Payment가 DB에 저장되지 않았는지 확인 (롤백)
        assertThat(paymentRepository.findByPgTransactionId("tgen-rollback-002"))
                .isEmpty();
    }

    // ════════════════════════════════════════════════════════════════════════
    // 3. 분산 락 - 동시 결제 방지
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @org.junit.jupiter.api.Order(3)
    @DisplayName("[통합] 분산 락 - 동일 주문 동시 결제 시 1건만 성공")
    void integration_distributedLock_concurrentPayment() throws InterruptedException {
        // given
        int threadCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 5개 스레드가 동시에 같은 주문 결제 시도
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

        // then - 1건만 성공, 나머지는 실패
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(threadCount - 1);

        // DB에 Payment가 1건만 저장됐는지 확인
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
        // given - 먼저 정상 결제
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .pgProvider("TEST")
                .pgTransactionId("tgen-cancel-001")
                .orderId(testOrderId)
                .amount(2800)
                .method("CARD")
                .build();
        paymentService.confirmPayment(paymentRequest);

        // 결제 완료 확인
        Order paidOrder = orderRepository.findById(testOrderId).orElseThrow();
        assertThat(paidOrder.getStatus()).isEqualTo(Order.OrderStatus.PAID);

        // when - 환불 요청
        PaymentCancelRequest cancelRequest = PaymentCancelRequest.builder()
                .pgTransactionId("tgen-cancel-001")
                .cancelReason("고객 요청")
                .build();
        PaymentCancelResponse response = paymentService.cancelPayment(cancelRequest);

        // then
        // 응답 확인
        assertThat(response.getCancelReason()).isEqualTo("고객 요청");
        assertThat(response.getRefundAmount()).isEqualTo(2800);

        // DB에서 실제 Order 상태 확인
        Order refundedOrder = orderRepository.findById(testOrderId).orElseThrow();
        assertThat(refundedOrder.getStatus()).isEqualTo(Order.OrderStatus.REFUNDED);
    }

    // ════════════════════════════════════════════════════════════════════════
    // 5. Redis 캐싱 확인
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @org.junit.jupiter.api.Order(5)
    @DisplayName("[통합] Redis 캐싱 - 메뉴 캐시가 실제 Redis에 저장됐는지 확인")
    void integration_redis_menuCacheExists() {
        // CacheWarmupRunner가 서버 시작 시 이미 캐싱함
        var cache = cacheManager.getCache("menus");
        assertThat(cache).isNotNull();

        var cachedMenus = cache.get("all");
        assertThat(cachedMenus).isNotNull();
        assertThat(cachedMenus.get()).isNotNull();
    }
}