package com.settleops.domain.order.api.dto;

import com.settleops.domain.order.domain.Orders;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class OrderCreateResponseDTO {

    private String orderId;
    private String status;
    private String merchantId;
    private String buyerId;
    private String itemName;
    private long amount;
    private String currency;
    private LocalDateTime createdAt;

    public static OrderCreateResponseDTO from(Orders order) {
        return new OrderCreateResponseDTO(
                order.getOrderId(),
                order.getStatus().name(),
                order.getMerchantId(),
                order.getBuyerId(),
                order.getItemName(),
                order.getAmount(),
                order.getCurrency(),
                order.getCreatedAt()
        );
    }
}