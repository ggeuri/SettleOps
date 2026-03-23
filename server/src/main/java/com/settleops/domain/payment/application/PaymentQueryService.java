package com.settleops.domain.payment.application;

import com.settleops.domain.payment.api.dto.MerchantPaymentListItemResponse;
import com.settleops.domain.payment.api.dto.MerchantPaymentSearchCondition;
import com.settleops.domain.payment.api.dto.PaymentDetailResponse;
import com.settleops.domain.payment.api.dto.RefundContextResponse;
import com.settleops.domain.payment.infra.PaymentQueryRepository;
import com.settleops.global.error.ForbiddenException;
import com.settleops.global.error.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentQueryService {

    private final PaymentQueryRepository paymentQueryRepository;

    public Page<MerchantPaymentListItemResponse> getMerchantPayments(
            String merchantId,
            String loginMerchantId,
            MerchantPaymentSearchCondition condition
    ) {
        validateMerchantAccess(merchantId, loginMerchantId);
        return paymentQueryRepository.searchMerchantPayments(merchantId, condition);
    }

    /**
     * 결제 상세 조회
     *
     * <p>IDOR 방지 정책:
     * <br>- 존재하지 않는 paymentId는 먼저 404로 수렴한다.
     * <br>- 존재가 확인된 이후 owner(merchantId) 일치 여부를 검증한다.
     * <br>- 이를 통해 미존재 자원과 권한 불일치 자원의 응답 기준을 분리한다.
     * </p>
     */
    public PaymentDetailResponse getPaymentDetail(String paymentId, String loginMerchantId) {
        PaymentDetailResponse detail = paymentQueryRepository.findPaymentDetail(paymentId);
        if (detail == null) {
            throw new NotFoundException("payment not found");
        }
        validateMerchantAccess(detail.getMerchantId(), loginMerchantId);
        return detail;
    }

    /**
     * 환불 컨텍스트 조회
     *
     * <p>IDOR 방지 정책:
     * <br>- 존재하지 않는 paymentId는 먼저 404로 수렴한다.
     * <br>- 존재가 확인된 이후 owner(merchantId) 일치 여부를 검증한다.
     * <br>- 이를 통해 미존재 자원과 권한 불일치 자원의 응답 기준을 분리한다.
     * </p>
     */
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