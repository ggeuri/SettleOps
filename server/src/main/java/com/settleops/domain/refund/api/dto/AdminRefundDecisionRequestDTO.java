package com.settleops.domain.refund.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AdminRefundDecisionRequestDTO {
    @NotBlank(message = "comment is required")
    private String comment;
}
