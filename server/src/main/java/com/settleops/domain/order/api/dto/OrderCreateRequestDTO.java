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

    @NotBlank(message = "merchantId는 필수입니다.")
    private String merchantId;

    @NotBlank(message = "buyerId는 필수입니다.")
    private String buyerId;

    @NotBlank(message = "itemName은 필수입니다.")
    private String itemName;

    @Positive(message = "amount는 0보다 커야 합니다.")
    private long amount;
}