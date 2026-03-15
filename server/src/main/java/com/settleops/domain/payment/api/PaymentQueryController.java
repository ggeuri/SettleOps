package com.settleops.domain.payment.api;

import com.settleops.domain.payment.api.dto.PaymentDetailResponse;
import com.settleops.domain.payment.api.dto.RefundContextResponse;
import com.settleops.domain.payment.application.PaymentQueryService;
import com.settleops.global.auth.annotation.LoginMerchant;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/payments")
public class PaymentQueryController {

    private final PaymentQueryService paymentQueryService;

    /**
     * 결제 상세 조회
     */
    @GetMapping("/{paymentId}")
    public PaymentDetailResponse getPaymentDetail(
            @PathVariable String paymentId,
            @LoginMerchant String loginMerchantId
    ) {
        return paymentQueryService.getPaymentDetail(paymentId, loginMerchantId);
    }

    /**
     * 환불 컨텍스트 조회
     */
    @GetMapping("/{paymentId}/refund-context")
    public RefundContextResponse getRefundContext(
            @PathVariable String paymentId,
            @LoginMerchant String loginMerchantId
    ) {
        return paymentQueryService.getRefundContext(paymentId, loginMerchantId);
    }
}