package com.settleops.domain.refund.application;

import com.settleops.domain.refund.infra.RefundSettlementLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefundAdjustmentEvidenceReader {

    private final RefundSettlementLinkRepository linkRepository;

    /**
     * LOCKED: REFUND_ADJUSTMENT_PENDING 판정 SoT는 refund_settlement_link 단일.
     * join/추론 금지.
     */
    public boolean existsEvidenceForSettlement(String settlementId) {
        return linkRepository.existsBySettlementId(settlementId);
    }
}