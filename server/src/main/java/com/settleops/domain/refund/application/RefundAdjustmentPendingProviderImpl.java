package com.settleops.domain.refund.application;

import com.settleops.domain.refund.infra.RefundAdjustmentPendingReadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * C 오너 구현.
 *
 * LOCKED:
 * - pending 판정의 "완료 증거"는 refund_settlement_link
 * - settlement 서비스는 이 Provider만 호출
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefundAdjustmentPendingProviderImpl implements RefundAdjustmentPendingProvider {

    private final RefundAdjustmentPendingReadRepository readRepository;

    @Override
    public boolean existsPendingForSettlement(String settlementId) {
        return readRepository.existsPendingApprovedRefundForSettlement(settlementId);
    }
}