package com.settleops.domain.refund.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundCreateRequestDTO {

    @NotBlank
    private String paymentId;   // UUID 문자열

    @Min(1)
    private long amount;        // KRW 정수

    @NotBlank
    private String reasonText;
}
