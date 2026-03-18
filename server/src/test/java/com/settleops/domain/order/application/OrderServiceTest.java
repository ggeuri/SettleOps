package com.settleops.domain.order.application;

import com.settleops.domain.order.domain.Orders;
import com.settleops.domain.order.infra.OrdersRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class OrderServiceTest {

    private final OrdersRepository ordersRepository = mock(OrdersRepository.class);
    private final OrderService orderService = new OrderService(ordersRepository);

    @Test
    @DisplayName("같은 입력으로 두 번 생성해도 중복 생성이 허용된다")
    void createSeedOrder_allowsDuplicateCreation() {
        given(ordersRepository.save(any(Orders.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        Orders first = orderService.createSeedOrder("merchant-1", "buyer-1", "아이템", 1000L);
        Orders second = orderService.createSeedOrder("merchant-1", "buyer-1", "아이템", 1000L);

        assertThat(first).isNotNull();
        assertThat(second).isNotNull();
        assertThat(first.getOrderId()).isNotEqualTo(second.getOrderId());
    }

    @Test
    @DisplayName("같은 입력으로 두 번 생성하면 save도 두 번 호출된다")
    void createSeedOrder_savesTwiceForDuplicateInput() {
        given(ordersRepository.save(any(Orders.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        orderService.createSeedOrder("merchant-1", "buyer-1", "아이템", 1000L);
        orderService.createSeedOrder("merchant-1", "buyer-1", "아이템", 1000L);

        verify(ordersRepository, times(2)).save(any(Orders.class));
    }
}