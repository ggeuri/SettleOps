package com.settleops.domain.refund.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundCreateRequestDTO {

    @NotBlank
    private String paymentId;   // UUID 문자열

    @NotNull
    @Positive
    private Long amount;        // KRW 정수

    @NotBlank
    private String reasonText;
}
