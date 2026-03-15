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
     *
     * 주의:
     * - 이 메서드는 settlement 단위의 "링크가 1건이라도 있나"만 본다.
     * - REFUND_ADJUSTMENT_PENDING 전체 판정에 직접 사용하면 안 된다.
     * - pending 판정은
     *   "해당 settlement에 연결된 APPROVED refund 중 link 없는 건이 존재하는지"
     *   를 전용 조회(RefundAdjustmentPendingReadRepository)로 계산해야 한다.
     */
    public boolean existsLinkForSettlement(String settlementId) {
        return linkRepository.existsBySettlementId(settlementId);
    }
}