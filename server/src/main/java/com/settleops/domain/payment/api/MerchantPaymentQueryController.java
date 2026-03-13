package com.settleops.domain.payment.api;

import com.settleops.domain.payment.api.dto.MerchantPaymentListItemResponse;
import com.settleops.domain.payment.api.dto.MerchantPaymentSearchCondition;
import com.settleops.domain.payment.application.PaymentQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/merchants/{merchantId}/payments")
public class MerchantPaymentQueryController {

    private final PaymentQueryService paymentQueryService;

    @GetMapping
    public List<MerchantPaymentListItemResponse> getMerchantPayments(
            @PathVariable String merchantId,
            MerchantPaymentSearchCondition condition
            // @AuthenticationPrincipal CustomUser user
    ) {
        // TODO: 로그인 주체 merchantId == path merchantId 검증
        return paymentQueryService.getMerchantPayments(merchantId, condition);
    }
}