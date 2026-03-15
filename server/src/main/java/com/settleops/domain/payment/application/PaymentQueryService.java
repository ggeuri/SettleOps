package com.settleops.domain.payment.application;

import com.settleops.domain.payment.api.dto.MerchantPaymentListItemResponse;
import com.settleops.domain.payment.api.dto.MerchantPaymentSearchCondition;
import com.settleops.domain.payment.api.dto.PaymentDetailResponse;
import com.settleops.domain.payment.api.dto.RefundContextResponse;
import com.settleops.domain.payment.infra.PaymentQueryRepository;
import com.settleops.global.error.ForbiddenException;
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
            String loginMerchantId,
            MerchantPaymentSearchCondition condition
    ) {
        validateMerchantAccess(merchantId, loginMerchantId);
        return paymentQueryRepository.searchMerchantPayments(merchantId, condition);
    }

    public PaymentDetailResponse getPaymentDetail(String paymentId, String loginMerchantId) {
        PaymentDetailResponse detail = paymentQueryRepository.findPaymentDetail(paymentId);
        if (detail == null) {
            throw new NotFoundException("payment not found");
        }
        validateMerchantAccess(detail.getMerchantId(), loginMerchantId);
        return detail;
    }

    public RefundContextResponse getRefundContext(String paymentId, String loginMerchantId) {
        RefundContextResponse context = paymentQueryRepository.findRefundContext(paymentId);
        if (context == null) {
            throw new NotFoundException("payment not found");
        }
        validateMerchantAccess(context.getMerchantId(), loginMerchantId);
        return context;
    }

    private void validateMerchantAccess(String merchantId, String loginMerchantId) {
        if (!merchantId.equals(loginMerchantId)) {
            throw new ForbiddenException("다른 상점의 데이터는 조회할 수 없습니다.");
        }
    }
}