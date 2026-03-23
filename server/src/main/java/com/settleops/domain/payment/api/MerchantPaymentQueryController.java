package com.settleops.domain.payment.api;

import com.settleops.domain.payment.api.dto.MerchantPaymentListItemResponse;
import com.settleops.domain.payment.api.dto.MerchantPaymentSearchCondition;
import com.settleops.domain.payment.application.PaymentQueryService;
import com.settleops.global.auth.annotation.LoginMerchant;
import com.settleops.global.web.pagination.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/merchants/{merchantId}/payments")
public class MerchantPaymentQueryController {

    private final PaymentQueryService paymentQueryService;

    @GetMapping
    public PageResponse<MerchantPaymentListItemResponse> getMerchantPayments(
            @PathVariable String merchantId,
            @ModelAttribute MerchantPaymentSearchCondition condition,
            @LoginMerchant String loginMerchantId
    ) {

        return PageResponse.from(
                paymentQueryService.getMerchantPayments(merchantId, loginMerchantId, condition)
        );
    }
}