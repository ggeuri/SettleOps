package com.settleops.domain.refund.application;

import com.settleops.domain.refund.infra.RefundReadRepository;
import com.settleops.domain.settlement.application.RefundAdjustmentPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementRefundAdjustmentPolicy implements RefundAdjustmentPolicy {

    private final RefundReadRepository refundReadRepository;
    private final RefundAdjustmentEvidenceReader refundAdjustmentEvidenceReader;

    @Override
    public boolean isRefundAdjustmentPending(String settlementId) {
        boolean hasApprovedRefund =
                refundReadRepository.existsRefundSettlementLinkEvidence(settlementId);

        if (!hasApprovedRefund) {
            return false;
        }

        boolean hasLink =
                refundAdjustmentEvidenceReader.existsLinkForSettlement(settlementId);

        return !hasLink;
    }
}
