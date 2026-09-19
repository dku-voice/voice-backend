package com.dku.voice.voice_backend;

import com.dku.voice.voice_backend.dto.*;
import com.dku.voice.voice_backend.entity.*;
import com.dku.voice.voice_backend.repository.*;
import com.dku.voice.voice_backend.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.dku.voice.voice_backend.TestFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoiceBackendApplicationTests {

    // ════════════════════════════════════════════════════════════════════════
    // OrderService 테스트
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("OrderService")
    class OrderServiceTest {

        @Mock OrderRepository orderRepository;
        @Mock MenuRepository menuRepository;
        @Mock MenuOptionRepository menuOptionRepository;
        @Mock KafkaProducerService kafkaProducerService;
        @InjectMocks OrderService orderService;

        @Test
        @DisplayName("정상 주문 생성 - 총금액이 서버에서 올바르게 계산된다")
        void createOrder_success() {
            Menu burger = activeMenu(1L, "불고기버거", 6000);
            Menu cola   = activeMenu(5L, "콜라", 3000);

            given(menuRepository.findById(1L)).willReturn(Optional.of(burger));
            given(menuRepository.findById(5L)).willReturn(Optional.of(cola));
            given(menuOptionRepository.findAllById(List.of())).willReturn(List.of());
            given(orderRepository.save(any())).willAnswer(i -> i.getArgument(0));
            doNothing().when(kafkaProducerService).sendOrderEvent(any());

            OrderRequest request = orderRequest(
                    new long[]{1L, 5L},
                    new int[]{2, 1}
            );

            OrderResponse response = orderService.createOrder(request);

            assertThat(response.getTotalPrice()).isEqualTo(15000);
            assertThat(response.getItems()).hasSize(2);
            verify(orderRepository).save(any(Order.class));
            verify(kafkaProducerService).sendOrderEvent(any());
        }

        @ParameterizedTest(name = "{0} 메뉴 주문 시도 → 예외 발생")
        @EnumSource(value = Menu.MenuStatus.class, names = {"SOLD_OUT", "INACTIVE"})
        @DisplayName("주문 불가 상태 메뉴 주문 시도 → 예외 발생")
        void createOrder_unavailableStatus_throwsException(Menu.MenuStatus status) {
            Menu unavailable = unavailableMenu(1L, status);
            given(menuRepository.findById(1L)).willReturn(Optional.of(unavailable));

            OrderRequest request = orderRequest(new long[]{1L}, new int[]{1});

            assertThatThrownBy(() -> orderService.createOrder(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("주문할 수 없는 메뉴입니다");
        }

        @Test
        @DisplayName("존재하지 않는 menuId 주문 시도 → 예외 발생")
        void createOrder_menuNotFound_throwsException() {
            given(menuRepository.findById(999L)).willReturn(Optional.empty());

            OrderRequest request = orderRequest(new long[]{999L}, new int[]{1});

            assertThatThrownBy(() -> orderService.createOrder(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("주문할 수 없는 메뉴입니다");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // PaymentService 테스트
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PaymentService")
    class PaymentServiceTest {

        @Mock PaymentRepository paymentRepository;
        @Mock OrderRepository orderRepository;
        @Mock RedissonClient redissonClient;
        @Mock RestClient restClient;
        @Mock RLock rLock;
        @InjectMocks PaymentService paymentService;

        @BeforeEach
        void setUp() throws InterruptedException {
            // self 주입
            ReflectionTestUtils.setField(paymentService, "self", paymentService);
            // 락 기본 설정
            lenient().when(redissonClient.getLock(anyString())).thenReturn(rLock);
            lenient().when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
            lenient().when(rLock.isHeldByCurrentThread()).thenReturn(true);
        }

        @Test
        @DisplayName("정상 결제 승인 → Order 상태가 PAID로 변경된다")
        void confirmPayment_success() throws InterruptedException {
            Order order = pendingOrder();
            given(orderRepository.findById(1001L)).willReturn(Optional.of(order));
            given(paymentRepository.findByPgTransactionId("tgen_test_001")).willReturn(Optional.empty());
            given(paymentRepository.save(any())).willAnswer(i -> i.getArgument(0));

            PaymentRequest request = paymentRequest("tgen_test_001", 1001L, 12000);

            PaymentResponse response = paymentService.confirmPayment(request);

            assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.PAID);
            assertThat(response.getAmount()).isEqualTo(12000);
            assertThat(response.getPgTransactionId()).isEqualTo("tgen_test_001");
        }

        @Test
        @DisplayName("위변조 검증 - 요청 금액이 주문 금액과 다르면 예외 발생")
        void confirmPayment_amountMismatch_throwsException() throws InterruptedException {
            Order order = pendingOrder();
            given(orderRepository.findById(1001L)).willReturn(Optional.of(order));

            PaymentRequest request = paymentRequest("tgen_test_002", 1001L, 1000);

            assertThatThrownBy(() -> paymentService.confirmPayment(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("결제 금액이 주문 금액과 다릅니다");
        }

        @Test
        @DisplayName("이미 PAID된 주문 재결제 시도 → 예외 발생")
        void confirmPayment_alreadyPaid_throwsException() throws InterruptedException {
            Order order = paidOrder();
            given(orderRepository.findById(1002L)).willReturn(Optional.of(order));

            PaymentRequest request = paymentRequest("tgen_test_003", 1002L, 12000);

            assertThatThrownBy(() -> paymentService.confirmPayment(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("이미 결제 완료된 주문입니다");
        }

        @Test
        @DisplayName("중복 pgTransactionId 결제 시도 → 예외 발생")
        void confirmPayment_duplicateTransactionId_throwsException() throws InterruptedException {
            Order order = pendingOrder();
            Payment existing = payment("tgen_duplicate", paidOrder());

            given(orderRepository.findById(1001L)).willReturn(Optional.of(order));
            given(paymentRepository.findByPgTransactionId("tgen_duplicate"))
                    .willReturn(Optional.of(existing));

            PaymentRequest request = paymentRequest("tgen_duplicate", 1001L, 12000);

            assertThatThrownBy(() -> paymentService.confirmPayment(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("이미 사용된 거래 ID입니다");
        }

        @Test
        @DisplayName("분산 락 획득 실패 → 예외 발생 (동시 결제 방지)")
        void confirmPayment_lockNotAcquired_throwsException() throws InterruptedException {
            given(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(false);

            PaymentRequest request = paymentRequest("tgen_lock_001", 1001L, 12000);

            assertThatThrownBy(() -> paymentService.confirmPayment(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("현재 결제가 진행 중입니다");
        }

        @Test
        @DisplayName("분산 락 획득 후 결제 완료 시 락이 정상 해제된다")
        void confirmPayment_lockReleasedAfterSuccess() throws InterruptedException {
            Order order = pendingOrder();
            given(orderRepository.findById(1001L)).willReturn(Optional.of(order));
            given(paymentRepository.findByPgTransactionId("tgen_lock_002")).willReturn(Optional.empty());
            given(paymentRepository.save(any())).willAnswer(i -> i.getArgument(0));

            PaymentRequest request = paymentRequest("tgen_lock_002", 1001L, 12000);

            paymentService.confirmPayment(request);

            verify(rLock).unlock();
        }

        @Test
        @DisplayName("결제 승인 중 예외 발생 시 Payment가 저장되지 않는다 (롤백)")
        void confirmPayment_exceptionDuringProcess_paymentNotSaved() throws InterruptedException {
            Order order = pendingOrder();
            given(orderRepository.findById(1001L)).willReturn(Optional.of(order));
            given(paymentRepository.findByPgTransactionId("tgen_rollback_001")).willReturn(Optional.empty());
            given(paymentRepository.save(any())).willThrow(new RuntimeException("DB 오류"));

            PaymentRequest request = paymentRequest("tgen_rollback_001", 1001L, 12000);

            assertThatThrownBy(() -> paymentService.confirmPayment(request))
                    .isInstanceOf(RuntimeException.class);

            assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.PENDING);
        }

        @Test
        @DisplayName("위변조 검증 실패 시 Payment가 저장되지 않는다 (롤백)")
        void confirmPayment_tamperingDetected_paymentNotSaved() throws InterruptedException {
            Order order = pendingOrder();
            given(orderRepository.findById(1001L)).willReturn(Optional.of(order));

            PaymentRequest request = paymentRequest("tgen_rollback_002", 1001L, 999);

            assertThatThrownBy(() -> paymentService.confirmPayment(request))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(paymentRepository, never()).save(any());
            assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.PENDING);
        }

        @Test
        @DisplayName("정상 환불 → Order 상태가 REFUNDED로 변경된다")
        void cancelPayment_success() {
            Order order = paidOrder();
            Payment existingPayment = payment("tgen_cancel_001", order);

            given(paymentRepository.findByPgTransactionId("tgen_cancel_001"))
                    .willReturn(Optional.of(existingPayment));

            PaymentCancelRequest request = cancelRequest("tgen_cancel_001", "고객 요청");

            PaymentCancelResponse response = paymentService.cancelPayment(request);

            assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.REFUNDED);
            assertThat(response.getCancelReason()).isEqualTo("고객 요청");
            assertThat(response.getRefundAmount()).isEqualTo(12000);
        }

        @ParameterizedTest(name = "주문 상태 {0} 환불 시도 → 예외 발생")
        @EnumSource(value = Order.OrderStatus.class, names = {"PENDING", "REFUNDED"})
        @DisplayName("PAID 아닌 주문 환불 시도 → 예외 발생")
        void cancelPayment_notPaid_throwsException(Order.OrderStatus status) {
            Order order = Order.builder()
                    .id(1001L).orderNumber("ORD-TEST")
                    .totalPrice(12000).status(status)
                    .build();
            Payment existingPayment = payment("tgen_cancel_002", order);

            given(paymentRepository.findByPgTransactionId("tgen_cancel_002"))
                    .willReturn(Optional.of(existingPayment));

            PaymentCancelRequest request = cancelRequest("tgen_cancel_002", "취소 요청");

            assertThatThrownBy(() -> paymentService.cancelPayment(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("환불 가능한 상태가 아닙니다");
        }

        @Test
        @DisplayName("존재하지 않는 pgTransactionId 환불 시도 → 예외 발생")
        void cancelPayment_notFound_throwsException() {
            given(paymentRepository.findByPgTransactionId("tgen_not_exist"))
                    .willReturn(Optional.empty());

            PaymentCancelRequest request = cancelRequest("tgen_not_exist", "취소 요청");

            assertThatThrownBy(() -> paymentService.cancelPayment(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("결제 내역을 찾을 수 없습니다");
        }
    }
}