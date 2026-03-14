package com.settleops.domain.settlement.application;

import com.settleops.domain.settlement.dto.AdminSettlementListItemResponse;
import com.settleops.domain.settlement.enums.SettlementStatus;
import com.settleops.domain.settlement.infra.SettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementQueryServiceImpl implements SettlementQueryService{

    private final SettlementRepository settlementRepository;

    @Override
    public Page<AdminSettlementListItemResponse> getAdminSettlements(
            SettlementStatus status,
            String merchantId,
            Pageable pageable
    ){
        return settlementRepository.searchAdminSettlements(status, merchantId, pageable);
    }
}
