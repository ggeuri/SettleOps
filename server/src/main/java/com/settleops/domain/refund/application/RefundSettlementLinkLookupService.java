package com.settleops.domain.refund.application;

import com.settleops.domain.refund.infra.RefundSettlementLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefundSettlementLinkLookupService {

    private final RefundSettlementLinkRepository linkRepository;

    /**
     * refund_settlement_link 보조 조회.
     *
     * 책임:
     * - 특정 settlementId에 대해 link 증거가 1건이라도 존재하는지만 확인한다.
     *
     * 비책임:
     * - REFUND_ADJUSTMENT_PENDING 최종 판정
     * - APPROVED refund 존재 여부 판정
     * - request-paid 가드 판정
     *
     * 주의:
     * - 이 메서드는 "settlement 단위 link 존재 여부"만 본다.
     * - pending 정책/운영 가드 용도로 사용하면 안 된다.
     */
    public boolean existsSettlementLinkEvidence(String settlementId) {
        return linkRepository.existsBySettlementId(settlementId);
    }
}