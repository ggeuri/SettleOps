package com.settleops.domain.refund.application;

import com.settleops.domain.refund.infra.RefundSettlementLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefundAdjustmentEvidenceReader {

    private final RefundSettlementLinkRepository linkRepository;

    /**
     * LOCKED:
     * refund_settlement_link(증거 테이블) 존재 여부만 확인.
     * REFUND_ADJUSTMENT_PENDING 전체 판정이 아님.
     * (APPROVED 존재 여부는 별도 로직)
     */
    public boolean existsLinkForSettlement(String settlementId) {
        return linkRepository.existsBySettlementId(settlementId);
    }
}