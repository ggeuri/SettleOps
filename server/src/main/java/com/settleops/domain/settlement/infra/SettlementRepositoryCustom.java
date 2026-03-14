package com.settleops.domain.settlement.infra;

import com.settleops.domain.settlement.dto.AdminSettlementListItemResponse;
import com.settleops.domain.settlement.enums.SettlementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SettlementRepositoryCustom {
    Page<AdminSettlementListItemResponse> searchAdminSettlements(
            SettlementStatus status,
            String merchantId,
            Pageable pageable
    );
}
