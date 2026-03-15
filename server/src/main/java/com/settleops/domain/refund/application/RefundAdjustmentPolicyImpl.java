package com.settleops.domain.refund.application;

import com.settleops.domain.settlement.application.RefundAdjustmentPolicy;
import com.settleops.global.error.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RefundAdjustmentPolicy 실제 구현체.
 *
 * LOCKED:
 * - request-paid 가드에서 사용
 * - A4 UI와 동일 정책을 공유
 * - settlement 서비스는 구현 상세를 모르고 이 정책만 호출
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefundAdjustmentPolicyImpl implements RefundAdjustmentPolicy {

    private final RefundAdjustmentPendingProvider pendingProvider;

    @Override
    public boolean isRefundAdjustmentPending(String settlementId) {
        if (settlementId == null || settlementId.isBlank()) {
            throw new BadRequestException("settlementId must not be null/blank");
        }

        return pendingProvider.existsPendingForSettlement(settlementId);
    }
}