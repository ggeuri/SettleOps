package com.settleops.domain.refund.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminRefundDecisionRequestDTO {
    @NotBlank(message = "comment is required")
    private String comment;
}
