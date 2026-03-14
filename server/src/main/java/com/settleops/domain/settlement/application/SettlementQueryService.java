package com.settleops.domain.settlement.application;

import com.settleops.domain.settlement.dto.AdminSettlementListItemResponse;
import com.settleops.domain.settlement.enums.SettlementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SettlementQueryService {
    Page<AdminSettlementListItemResponse> getAdminSettlements(
            SettlementStatus status,
            String merchantId,
            Pageable pageable
    );
}
