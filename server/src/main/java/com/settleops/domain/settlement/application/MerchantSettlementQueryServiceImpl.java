package com.settleops.domain.settlement.application;

import com.settleops.domain.settlement.dto.MerchantSettlementDetailResponse;
import com.settleops.domain.settlement.dto.MerchantSettlementListItemResponse;
import com.settleops.domain.settlement.infra.SettlementRepository;
import com.settleops.global.error.BadRequestException;
import com.settleops.global.error.ForbiddenException;
import com.settleops.global.error.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MerchantSettlementQueryServiceImpl implements MerchantSettlementQueryService {

    private final SettlementRepository settlementRepository;

    @Override
    public Page<MerchantSettlementListItemResponse> getMerchantSettlements(String loginMerchantId, String merchantId, Pageable pageable) {
        validateMerchantAccess(loginMerchantId, merchantId);
        return settlementRepository.searchMerchantSettlements(merchantId, pageable);
    }

    @Override
    public MerchantSettlementDetailResponse getMerchantSettlementDetail(String loginMerchantId, String merchantId, String settlementId) {
        validateMerchantAccess(loginMerchantId, merchantId);

        if (settlementId == null || settlementId.isBlank()) {
            throw new BadRequestException("settlementId must not be null/blank");
        }

        String ownerMerchantId = settlementRepository.findMerchantIdBySettlementId(settlementId)
                .orElseThrow(() -> new NotFoundException("settlement not found"));

        if (!ownerMerchantId.equals(loginMerchantId)) {
            throw new ForbiddenException("merchant mismatch");
        }

        MerchantSettlementDetailResponse response =
                settlementRepository.findMerchantSettlementDetail(merchantId, settlementId);

        if (response == null) {
            throw new NotFoundException("settlement not found");
        }

        return response;
    }

    private void validateMerchantAccess(String loginMerchantId, String merchantId) {
        if (loginMerchantId == null || loginMerchantId.isBlank()) {
            throw new BadRequestException("loginMerchantId must not be null/blank");
        }

        if (merchantId == null || merchantId.isBlank()) {
            throw new BadRequestException("merchantId must not be a null/blank");
        }

        if (!loginMerchantId.equals(merchantId)) {
            throw new ForbiddenException("merchant mismatch");
        }
    }
}