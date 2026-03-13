package com.settleops.domain.payment.application;

import com.settleops.domain.payment.api.dto.MerchantPaymentListItemResponse;
import com.settleops.domain.payment.api.dto.MerchantPaymentSearchCondition;
import com.settleops.domain.payment.api.dto.PaymentDetailResponse;
import com.settleops.domain.payment.api.dto.RefundContextResponse;
import com.settleops.domain.payment.infra.PaymentQueryRepository;
import com.settleops.global.error.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentQueryService {

    private final PaymentQueryRepository paymentQueryRepository;

    public List<MerchantPaymentListItemResponse> getMerchantPayments(
            String merchantId,
            MerchantPaymentSearchCondition condition
    ) {
        return paymentQueryRepository.searchMerchantPayments(merchantId, condition);
    }

    public PaymentDetailResponse getPaymentDetail(String paymentId) {
        PaymentDetailResponse detail = paymentQueryRepository.findPaymentDetail(paymentId);
        if (detail == null) {
            throw new NotFoundException("payment not found");
        }
        return detail;    }

    public RefundContextResponse getRefundContext(String paymentId) {
        RefundContextResponse context = paymentQueryRepository.findRefundContext(paymentId);
        if (context == null) {
            throw new NotFoundException("payment not found");
        }
        return context;    }
}