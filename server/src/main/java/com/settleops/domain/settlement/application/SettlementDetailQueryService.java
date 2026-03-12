package com.settleops.domain.settlement.application;

import com.settleops.domain.settlement.dto.AdminSettlementDetailResponse;

public interface SettlementDetailQueryService {
    AdminSettlementDetailResponse getAdminSettlementDetail(String settlementId);
}
