package com.dku.voice.voice_backend;

import com.dku.voice.voice_backend.dto.*;
import com.dku.voice.voice_backend.entity.*;
import com.dku.voice.voice_backend.repository.*;
import com.dku.voice.voice_backend.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static com.dku.voice.voice_backend.TestFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

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
        @InjectMocks OrderService orderService;

        @Test
        @DisplayName("정상 주문 생성 - 총금액이 서버에서 올바르게 계산된다")
        void createOrder_success() {
            // given
            // 불고기버거(6000) x2 + 콜라(3000) x1 = 15000
            Menu burger = activeMenu(1L, "불고기버거", 6000);
            Menu cola   = activeMenu(5L, "콜라", 3000);

            given(menuRepository.findById(1L)).willReturn(Optional.of(burger));
            given(menuRepository.findById(5L)).willReturn(Optional.of(cola));
            given(orderRepository.save(any())).willAnswer(i -> i.getArgument(0));

            OrderRequest request = orderRequest(
                    new long[]{1L, 5L},
                    new int[]{2, 1}
            );

            // when
            OrderResponse response = orderService.createOrder(request);

            // then
            assertThat(response.getTotalPrice()).isEqualTo(15000);
            assertThat(response.getItems()).hasSize(2);
            verify(orderRepository).save(any(Order.class));
        }

        @ParameterizedTest(name = "{0} 메뉴 주문 시도 → 예외 발생")
        @EnumSource(value = Menu.MenuStatus.class, names = {"SOLD_OUT", "INACTIVE"})
        @DisplayName("주문 불가 상태 메뉴 주문 시도 → 예외 발생")
        void createOrder_unavailableStatus_throwsException(Menu.MenuStatus status) {
            // given
            Menu unavailable = unavailableMenu(1L, status);
            given(menuRepository.findById(1L)).willReturn(Optional.of(unavailable));

            OrderRequest request = orderRequest(new long[]{1L}, new int[]{1});

            // when & then
            assertThatThrownBy(() -> orderService.createOrder(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("주문할 수 없는 메뉴입니다");
        }

        @Test
        @DisplayName("존재하지 않는 menuId 주문 시도 → 예외 발생")
        void createOrder_menuNotFound_throwsException() {
            // given
            given(menuRepository.findById(999L)).willReturn(Optional.empty());

            OrderRequest request = orderRequest(new long[]{999L}, new int[]{1});

            // when & then
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
        @InjectMocks PaymentService paymentService;

        @Test
        @DisplayName("정상 결제 승인 → Order 상태가 PAID로 변경된다")
        void confirmPayment_success() {
            // given
            Order order = pendingOrder();
            given(orderRepository.findById(1001L)).willReturn(Optional.of(order));
            given(paymentRepository.findByPgTransactionId("tgen_test_001")).willReturn(Optional.empty());
            given(paymentRepository.save(any())).willAnswer(i -> i.getArgument(0));

            PaymentRequest request = paymentRequest("tgen_test_001", 1001L, 12000);

            // when
            PaymentResponse response = paymentService.confirmPayment(request);

            // then
            assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.PAID);
            assertThat(response.getAmount()).isEqualTo(12000);
            assertThat(response.getPgTransactionId()).isEqualTo("tgen_test_001");
        }

        @Test
        @DisplayName("위변조 검증 - 요청 금액이 주문 금액과 다르면 예외 발생")
        void confirmPayment_amountMismatch_throwsException() {
            // given
            Order order = pendingOrder(); // totalPrice = 12000
            given(orderRepository.findById(1001L)).willReturn(Optional.of(order));

            // 실제 주문금액 12000, 요청금액 1000 (위변조 시도)
            PaymentRequest request = paymentRequest("tgen_test_002", 1001L, 1000);

            // when & then
            assertThatThrownBy(() -> paymentService.confirmPayment(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("결제 금액이 주문 금액과 다릅니다");
        }

        @Test
        @DisplayName("이미 PAID된 주문 재결제 시도 → 예외 발생")
        void confirmPayment_alreadyPaid_throwsException() {
            // given
            Order order = paidOrder();
            given(orderRepository.findById(1002L)).willReturn(Optional.of(order));

            PaymentRequest request = paymentRequest("tgen_test_003", 1002L, 12000);

            // when & then
            assertThatThrownBy(() -> paymentService.confirmPayment(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("이미 결제 완료된 주문입니다");
        }

        @Test
        @DisplayName("중복 pgTransactionId 결제 시도 → 예외 발생")
        void confirmPayment_duplicateTransactionId_throwsException() {
            // given
            Order order = pendingOrder();
            Payment existing = payment("tgen_duplicate", paidOrder());

            given(orderRepository.findById(1001L)).willReturn(Optional.of(order));
            given(paymentRepository.findByPgTransactionId("tgen_duplicate"))
                    .willReturn(Optional.of(existing));

            PaymentRequest request = paymentRequest("tgen_duplicate", 1001L, 12000);

            // when & then
            assertThatThrownBy(() -> paymentService.confirmPayment(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("이미 사용된 거래 ID입니다");
        }

        @Test
        @DisplayName("정상 환불 → Order 상태가 REFUNDED로 변경된다")
        void cancelPayment_success() {
            // given
            Order order = paidOrder();
            Payment existingPayment = payment("tgen_cancel_001", order);

            given(paymentRepository.findByPgTransactionId("tgen_cancel_001"))
                    .willReturn(Optional.of(existingPayment));

            PaymentCancelRequest request = cancelRequest("tgen_cancel_001", "고객 요청");

            // when
            PaymentCancelResponse response = paymentService.cancelPayment(request);

            // then
            assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.REFUNDED);
            assertThat(response.getCancelReason()).isEqualTo("고객 요청");
            assertThat(response.getRefundAmount()).isEqualTo(12000);
        }

        @ParameterizedTest(name = "주문 상태 {0} 환불 시도 → 예외 발생")
        @EnumSource(value = Order.OrderStatus.class, names = {"PENDING", "REFUNDED"})
        @DisplayName("PAID 아닌 주문 환불 시도 → 예외 발생")
        void cancelPayment_notPaid_throwsException(Order.OrderStatus status) {
            // given
            Order order = Order.builder()
                    .id(1001L).orderNumber("ORD-TEST")
                    .totalPrice(12000).status(status)
                    .build();
            Payment existingPayment = payment("tgen_cancel_002", order);

            given(paymentRepository.findByPgTransactionId("tgen_cancel_002"))
                    .willReturn(Optional.of(existingPayment));

            PaymentCancelRequest request = cancelRequest("tgen_cancel_002", "취소 요청");

            // when & then
            assertThatThrownBy(() -> paymentService.cancelPayment(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("환불 가능한 상태가 아닙니다");
        }

        @Test
        @DisplayName("존재하지 않는 pgTransactionId 환불 시도 → 예외 발생")
        void cancelPayment_notFound_throwsException() {
            // given
            given(paymentRepository.findByPgTransactionId("tgen_not_exist"))
                    .willReturn(Optional.empty());

            PaymentCancelRequest request = cancelRequest("tgen_not_exist", "취소 요청");

            // when & then
            assertThatThrownBy(() -> paymentService.cancelPayment(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("결제 내역을 찾을 수 없습니다");
        }
    }
}