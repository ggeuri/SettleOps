package com.settleops.domain.hold.api.dto;

import com.settleops.domain.hold.domain.HoldReasonCode;
import com.settleops.domain.hold.domain.HoldStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class AdminHoldQueueRowDto {

    private final String holdId;
    private final String settlementId;
    private final String merchantId;

    private final HoldStatus status;
    private final HoldReasonCode reasonCode;
    private final String requestedComment;

    private final LocalDateTime createdAt;
}