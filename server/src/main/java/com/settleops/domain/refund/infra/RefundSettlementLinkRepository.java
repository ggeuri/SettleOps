package com.settleops.domain.refund.infra;

import com.settleops.domain.refund.domain.RefundSettlementLink;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundSettlementLinkRepository extends JpaRepository<RefundSettlementLink, Long> {
    //jpa
    boolean existsBySettlementId(String settlementId);
}
