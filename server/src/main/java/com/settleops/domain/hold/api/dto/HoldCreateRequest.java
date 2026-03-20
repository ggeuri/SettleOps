package com.settleops.domain.hold.api.dto;

import com.settleops.domain.hold.domain.HoldReasonCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record HoldCreateRequest(
        @NotBlank String settlementId,
        @NotNull HoldReasonCode reasonCode,
        @NotBlank String comment
) {}