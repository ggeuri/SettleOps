package com.settleops.domain.settlement.application;

import com.settleops.domain.settlement.dto.AdminSettlementDetailBaseView;
import com.settleops.domain.settlement.dto.AdminSettlementDetailResponse;
import com.settleops.domain.settlement.dto.AdminSettlementHoldSummaryResponse;
import com.settleops.domain.settlement.dto.AdminSettlementLineItemResponse;
import com.settleops.domain.settlement.dto.AdminSettlementRefundSummaryResponse;
import com.settleops.domain.settlement.infra.SettlementDetailQueryRepository;
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
public class SettlementDetailQueryServiceImpl implements SettlementDetailQueryService {

    private final SettlementDetailQueryRepository settlementDetailQueryRepository;
    private final RefundAdjustmentPolicy refundAdjustmentPolicy;

    @Override
    public AdminSettlementDetailResponse getAdminSettlementDetail(String settlementId) {
        if (settlementId == null || settlementId.isBlank()) {
            throw new BadRequestException("settlementId must not be null/blank");
        }

        AdminSettlementDetailBaseView base =
                settlementDetailQueryRepository.findSettlementBase(settlementId);

        if (base == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "settlement not found");
        }

        List<AdminSettlementLineItemResponse> lines =
                settlementDetailQueryRepository.findSettlementLines(settlementId);

        AdminSettlementHoldSummaryResponse hold =
                settlementDetailQueryRepository.findHoldSummary(settlementId);

        boolean hasApprovedRefund =
                settlementDetailQueryRepository.hasApprovedRefund(settlementId);

        boolean refundAdjustmentPending =
                refundAdjustmentPolicy.isRefundAdjustmentPending(settlementId);

        AdminSettlementRefundSummaryResponse refund =
                new AdminSettlementRefundSummaryResponse(
                        hasApprovedRefund,
                        refundAdjustmentPending
                );

        return new AdminSettlementDetailResponse(
                base.settlementId(),
                base.merchantId(),
                base.baseDate(),
                base.status(),
                base.gross(),
                base.fee(),
                base.vat(),
                base.net(),
                lines,
                hold,
                refund
        );
    }
}