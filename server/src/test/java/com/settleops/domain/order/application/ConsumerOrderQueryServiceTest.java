package com.settleops.domain.order.application;

import com.settleops.domain.order.api.dto.ConsumerOrderDetailResponse;
import com.settleops.domain.order.infra.ConsumerOrderQueryRepository;
import com.settleops.global.error.ForbiddenException;
import com.settleops.global.error.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.settleops.domain.order.api.dto.ConsumerOrderListItemResponse;
import com.settleops.domain.order.api.dto.ConsumerOrderListResponse;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ConsumerOrderQueryServiceTest {

    @Mock
    private ConsumerOrderQueryRepository consumerOrderQueryRepository;

    @InjectMocks
    private ConsumerOrderQueryService consumerOrderQueryService;

    @Test
    @DisplayName("본인 주문이면 상세 조회에 성공한다")
    void getOrderDetail_success() {
        // given
        String orderId = "550e8400-e29b-41d4-a716-446655440010";
        String loginConsumer = "buyer_2001";

        ConsumerOrderDetailResponse detail = new ConsumerOrderDetailResponse(
                orderId,
                "m_1001",
                "buyer_2001",
                "아이폰 14 프로",
                125000L,
                "CREATED",
                "550e8400-e29b-41d4-a716-446655440001",
                "CAPTURED",
                LocalDateTime.of(2026, 3, 18, 10, 25, 0),
                LocalDateTime.of(2026, 3, 18, 10, 30, 0),
                List.of(
                        new ConsumerOrderDetailResponse.PaymentEventItem(
                                "PAYMENT_CREATED",
                                null,
                                "CREATED",
                                LocalDateTime.of(2026, 3, 18, 10, 24, 58)
                        ),
                        new ConsumerOrderDetailResponse.PaymentEventItem(
                                "PAYMENT_CAPTURED",
                                "CREATED",
                                "CAPTURED",
                                LocalDateTime.of(2026, 3, 18, 10, 25, 0)
                        )
                )
        );

        given(consumerOrderQueryRepository.findConsumerOrderDetail(orderId))
                .willReturn(detail);

        // when
        ConsumerOrderDetailResponse result =
                consumerOrderQueryService.getOrderDetail(orderId, loginConsumer);

        // then
        assertThat(result.orderId()).isEqualTo(orderId);
        assertThat(result.buyerId()).isEqualTo("buyer_2001");
        assertThat(result.itemName()).isEqualTo("아이폰 14 프로");
        assertThat(result.paymentId()).isEqualTo("550e8400-e29b-41d4-a716-446655440001");
        assertThat(result.paymentStatus()).isEqualTo("CAPTURED");
        assertThat(result.events()).hasSize(2);
        then(consumerOrderQueryRepository)
                .should()
                .findConsumerOrderDetail(orderId);
    }

    @Test
    @DisplayName("존재하지 않는 주문이면 404 예외를 던진다")
    void getOrderDetail_notFound() {
        // given
        String orderId = "550e8400-e29b-41d4-a716-446655449999";
        String loginConsumer = "buyer_2001";

        given(consumerOrderQueryRepository.findConsumerOrderDetail(orderId))
                .willReturn(null);

        // when // then
        assertThatThrownBy(() -> consumerOrderQueryService.getOrderDetail(orderId, loginConsumer))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("order not found");
    }

    @Test
    @DisplayName("다른 구매자의 주문이면 403 예외를 던진다")
    void getOrderDetail_forbidden() {
        // given
        String orderId = "550e8400-e29b-41d4-a716-446655440010";
        String loginConsumer = "buyer_9999";

        ConsumerOrderDetailResponse detail = new ConsumerOrderDetailResponse(
                orderId,
                "m_1001",
                "buyer_2001",
                "아이폰 14 프로",
                125000L,
                "CREATED",
                null,
                null,
                null,
                null,
                List.of()
        );

        given(consumerOrderQueryRepository.findConsumerOrderDetail(orderId))
                .willReturn(detail);

        // when // then
        assertThatThrownBy(() -> consumerOrderQueryService.getOrderDetail(orderId, loginConsumer))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("다른 구매자의 주문은 조회할 수 없습니다.");
    }

    @Test
    @DisplayName("본인 주문 목록 조회에 성공한다")
    void getOrders_success() {
        // given
        String loginConsumer = "buyer_2001";

        List<ConsumerOrderListItemResponse> items = List.of(
                new ConsumerOrderListItemResponse(
                        "550e8400-e29b-41d4-a716-446655440010",
                        "아이폰 14 프로",
                        125000L,
                        "PAID",
                        true,
                        true,
                        LocalDateTime.of(2026, 3, 18, 10, 30, 0)
                )
        );

        given(consumerOrderQueryRepository.findConsumerOrders(loginConsumer, null, null))
                .willReturn(items);

        // when
        ConsumerOrderListResponse result =
                consumerOrderQueryService.getOrders(loginConsumer, null, null);

        // then
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).orderId()).isEqualTo("550e8400-e29b-41d4-a716-446655440010");
        assertThat(result.items().get(0).itemName()).isEqualTo("아이폰 14 프로");
        assertThat(result.items().get(0).paid()).isTrue();
        assertThat(result.items().get(0).confirmed()).isTrue();
        then(consumerOrderQueryRepository)
                .should()
                .findConsumerOrders(loginConsumer, null, null);
    }

    @Test
    @DisplayName("주문 목록이 없어도 빈 목록을 반환한다")
    void getOrders_emptyList() {
        // given
        String loginConsumer = "buyer_2001";

        given(consumerOrderQueryRepository.findConsumerOrders(loginConsumer, null, null))
                .willReturn(List.of());

        // when
        ConsumerOrderListResponse result =
                consumerOrderQueryService.getOrders(loginConsumer, null, null);

        // then
        assertThat(result.items()).isEmpty();
        then(consumerOrderQueryRepository)
                .should()
                .findConsumerOrders(loginConsumer, null, null);
    }
}