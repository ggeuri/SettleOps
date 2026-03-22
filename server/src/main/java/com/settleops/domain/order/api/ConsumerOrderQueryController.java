package com.settleops.domain.order.api;

import com.settleops.domain.order.api.dto.ConsumerOrderDetailResponse;
import com.settleops.domain.order.api.dto.ConsumerOrderListResponse;
import com.settleops.domain.order.application.ConsumerOrderQueryService;
import com.settleops.domain.order.domain.OrderStatus;
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

    /**
     * Consumer 주문/결제 목록 조회
     *
     * - status는 orders.status SoT 기준(CREATED, PAID)만 허용한다.
     * - confirmed는 PAYMENT_CONFIRMED 이벤트 존재 여부 기준으로 필터링한다.
     *   - CONFIRMED   : PAYMENT_CONFIRMED 이벤트 존재
     *   - UNCONFIRMED : PAYMENT_CONFIRMED 이벤트 미존재
     *   - ALL/null    : 조건 미적용
     * - 잘못된 status 입력은 400(BAD_REQUEST)로 처리된다.
     */
    @GetMapping
    public ConsumerOrderListResponse getOrders(
            @LoginConsumer String loginConsumerId,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String confirmed,
            @RequestParam(required = false) String keyword
    ) {
        return consumerOrderQueryService.getOrders(loginConsumerId, status, confirmed, keyword);
    }
}