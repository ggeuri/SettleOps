package com.settleops.domain.settlement.infra;

import com.settleops.domain.settlement.dto.AdminSettlementListItemResponse;
import com.settleops.domain.settlement.dto.MerchantSettlementDetailResponse;
import com.settleops.domain.settlement.dto.MerchantSettlementListItemResponse;
import com.settleops.domain.settlement.enums.SettlementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Optional;

public interface SettlementRepositoryCustom {
    Page<AdminSettlementListItemResponse> searchAdminSettlements(
            SettlementStatus status,
            String merchantId,
            Pageable pageable
    );

    Page<MerchantSettlementListItemResponse> searchMerchantSettlements(
            String merchantId,
            SettlementStatus status,
            LocalDate from,
            LocalDate to,
            Pageable pageable
    );

    Optional<String> findMerchantIdBySettlementId(String settlementId);

    MerchantSettlementDetailResponse findMerchantSettlementDetail(
            String merchantId,
            String settlementId
    );
}