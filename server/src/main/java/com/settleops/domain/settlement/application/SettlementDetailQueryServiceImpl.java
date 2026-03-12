package com.settleops.domain.settlement.application;

import com.settleops.domain.settlement.dto.AdminSettlementDetailResponse;
import com.settleops.domain.settlement.dto.AdminSettlementHoldSummaryResponse;
import com.settleops.domain.settlement.dto.AdminSettlementLineItemResponse;
import com.settleops.domain.settlement.dto.AdminSettlementRefundSummaryResponse;
import com.settleops.domain.settlement.entity.Settlement;
import com.settleops.domain.settlement.entity.SettlementLine;
import com.settleops.domain.settlement.infra.SettlementLineRepository;
import com.settleops.domain.settlement.infra.SettlementRepository;
import com.settleops.global.error.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementDetailQueryServiceImpl implements SettlementDetailQueryService{

    private final SettlementRepository settlementRepository;
    private final SettlementLineRepository settlementLineRepository;

    @Override
    public AdminSettlementDetailResponse getAdminSettlementDetail(String settlementId) {
        if (settlementId == null || settlementId.isBlank()) {
            throw new BadRequestException("settlementId must not be null/blank");
        }

        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "settlement not found"));

        List<AdminSettlementLineItemResponse> lines = settlementLineRepository
                .findBySettlementIdOrderByCreatedAtAsc(settlementId)
                .stream()
                .map(this::toLineItemResponse)
                .toList();

        return new AdminSettlementDetailResponse(
                settlement.getSettlementId(),
                settlement.getMerchantId(),
                settlement.getBaseDate(),
                settlement.getStatus(),
                settlement.getGross(),
                settlement.getFee(),
                settlement.getVat(),
                settlement.getNet(),
                lines,
                AdminSettlementHoldSummaryResponse.empty(),
                AdminSettlementRefundSummaryResponse.empty()
        );
    }

    private AdminSettlementLineItemResponse toLineItemResponse(SettlementLine line) {
        return new AdminSettlementLineItemResponse(
                String.valueOf(line.getSettlementLineId()),
                line.getLineType(),
                line.getPaymentId(),
                line.getAmount()
        );
    }
}
