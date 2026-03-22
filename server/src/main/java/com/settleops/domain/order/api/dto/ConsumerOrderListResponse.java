package com.settleops.domain.order.api.dto;

import java.util.List;

public record ConsumerOrderListResponse(
        List<ConsumerOrderListItemResponse> items
) {
}