package com.settleops.domain.refund.application;

import com.settleops.domain.refund.infra.RefundSettlementReadRepository;
import com.settleops.domain.settlement.application.RefundAdjustmentPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementRefundAdjustmentPolicy implements RefundAdjustmentPolicy {

    private final RefundSettlementReadRepository refundSettlementReadRepository;

    @Override
    public boolean isRefundAdjustmentPending(String settlementId) {
        return refundSettlementReadRepository.existsPendingApprovedRefundBySettlementId(settlementId);
    }
}