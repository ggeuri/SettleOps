package com.settleops.domain.payment.api;

import com.settleops.domain.payment.api.dto.PaymentDetailResponse;
import com.settleops.domain.payment.api.dto.RefundContextResponse;
import com.settleops.domain.payment.application.PaymentQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
            @PathVariable String paymentId
            // @AuthenticationPrincipal CustomUser user
    ) {
        // TODO: role별 소유 검증 적용
        return paymentQueryService.getPaymentDetail(paymentId);
    }

    /**
     * 환불 컨텍스트 조회
     */
    @GetMapping("/{paymentId}/refund-context")
    public RefundContextResponse getRefundContext(
            @PathVariable String paymentId
            // @AuthenticationPrincipal CustomUser user
    ) {
        // TODO: role별 소유 검증 적용
        return paymentQueryService.getRefundContext(paymentId);
    }
}