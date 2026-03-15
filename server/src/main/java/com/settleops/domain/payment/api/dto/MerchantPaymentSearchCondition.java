package com.settleops.domain.payment.api.dto;

import com.settleops.domain.payment.domain.PaymentStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class MerchantPaymentSearchCondition {

    private PaymentStatus status;
    private ConfirmedFilter confirmed; // ALL / CONFIRMED

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate from;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate to;

    private String keyword;
}