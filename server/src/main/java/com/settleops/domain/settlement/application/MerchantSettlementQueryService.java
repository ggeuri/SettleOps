package com.settleops.domain.settlement.application;

import com.settleops.domain.settlement.dto.MerchantSettlementDetailResponse;
import com.settleops.domain.settlement.dto.MerchantSettlementListItemResponse;
import com.settleops.domain.settlement.enums.SettlementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface MerchantSettlementQueryService {

    Page<MerchantSettlementListItemResponse> getMerchantSettlements(
            String loginMerchantId,
            String merchantId,
            SettlementStatus status,
            LocalDate from,
            LocalDate to,
            Pageable pageable
    );

    MerchantSettlementDetailResponse getMerchantSettlementDetail(
            String loginMerchantId,
            String merchantId,
            String settlementId
    );
}