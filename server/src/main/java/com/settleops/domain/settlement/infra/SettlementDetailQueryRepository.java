package com.settleops.domain.settlement.infra;

import com.settleops.domain.settlement.dto.AdminSettlementDetailBaseView;
import com.settleops.domain.settlement.dto.AdminSettlementHoldSummaryResponse;
import com.settleops.domain.settlement.dto.AdminSettlementLineItemResponse;
import com.settleops.domain.settlement.dto.AdminSettlementRefundSummaryResponse;

import java.util.List;

public interface SettlementDetailQueryRepository {
    AdminSettlementDetailBaseView findSettlementBase(String settlementId);

    List<AdminSettlementLineItemResponse> findSettlementLines(String settlementId);

    AdminSettlementHoldSummaryResponse findHoldSummary(String settlementId);

    boolean hasApprovedRefund(String settlementId);
}
