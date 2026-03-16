package com.settleops.domain.refund.infra;

import com.settleops.domain.refund.domain.RefundSettlementLink;
import org.springframework.data.jpa.repository.JpaRepository;
/**
 * READ 전용.
 *
 * 책임:
 * - KST(Asia/Seoul) baseDate 당일 00:00 이전에 승인 완료된
 *   APPROVED + unlinked refund 입력집합 조회
 *
 * LOCKED:
 * - cutoff 비교는 decidedAt < cutoff
 * - cutoff는 baseDate 당일 00:00
 * - refund_settlement_link 존재 시 제외
 */
public interface RefundSettlementLinkRepository extends JpaRepository<RefundSettlementLink, String> {
    //jpa
    boolean existsBySettlementId(String settlementId);
}
