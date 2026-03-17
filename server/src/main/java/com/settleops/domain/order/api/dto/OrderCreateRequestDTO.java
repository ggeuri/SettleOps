package com.settleops.domain.order.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateRequestDTO {

    @NotBlank
    private String merchantId;

    @NotBlank
    private String buyerId;

    @NotBlank
    private String itemName;

    @Positive
    private long amount;
}