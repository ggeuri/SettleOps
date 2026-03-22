package com.settleops.domain.hold.api.dto;

import com.settleops.domain.hold.domain.HoldStatus;

public record HoldQueueSearchRequestDto(
        HoldStatus status,
        String settlementId,
        String merchantId
) {
}