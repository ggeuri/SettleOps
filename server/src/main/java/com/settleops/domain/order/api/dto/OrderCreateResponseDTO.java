package com.settleops.domain.order.api.dto;

import com.settleops.domain.order.domain.Orders;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderCreateResponseDTO {

    private String orderId;
    private String status;
    private long amount;

    public static OrderCreateResponseDTO from(Orders order) {
        return new OrderCreateResponseDTO(
                order.getOrderId(),
                order.getStatus().name(),
                order.getAmount()
        );
    }
}