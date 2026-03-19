package com.settleops.domain.order.api;

import com.settleops.domain.order.api.dto.ConsumerOrderDetailResponse;
import com.settleops.domain.order.application.ConsumerOrderQueryService;
import com.settleops.global.auth.annotation.LoginConsumer;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/consumer/orders")
public class ConsumerOrderQueryController {

    private final ConsumerOrderQueryService consumerOrderQueryService;

    /**
     * Consumer 주문 상세 조회
     */
    @GetMapping("/{orderId}")
    public ConsumerOrderDetailResponse getOrderDetail(
            @PathVariable String orderId,
            @LoginConsumer String loginConsumerId
    ) {
        return consumerOrderQueryService.getOrderDetail(orderId, loginConsumerId);
    }
}