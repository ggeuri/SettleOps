package com.settleops.domain.settlement.application;

import com.settleops.domain.settlement.dto.MerchantSettlementDetailResponse;
import com.settleops.domain.settlement.dto.MerchantSettlementListItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MerchantSettlementQueryService {

    Page<MerchantSettlementListItemResponse> getMerchantSettlements(
            String loginMerchantId,
            String merchantId,
            Pageable pageable
    );

    MerchantSettlementDetailResponse getMerchantSettlementDetail(
            String loginMerchantId,
            String merchantId,
            String settlementId
    );
}
