package com.settleops.domain.settlement.application;

import com.settleops.domain.settlement.dto.AdminSettlementDetailBaseView;
import com.settleops.domain.settlement.dto.SettlementTraceEntryResponse;
import com.settleops.domain.settlement.infra.SettlementDetailQueryRepository;
import com.settleops.global.error.BadRequestException;
import com.settleops.global.error.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementTraceEntryQueryService {

    private final SettlementDetailQueryRepository settlementDetailQueryRepository;

    public SettlementTraceEntryResponse getSettlementTraceEntry(String settlementId) {
        if (settlementId == null || settlementId.isBlank()) {
            throw new BadRequestException("settlementId must not be null/blank");
        }

        AdminSettlementDetailBaseView base =
                settlementDetailQueryRepository.findSettlementBase(settlementId);

        if (base == null) {
            throw new NotFoundException("settlement not found");
        }

        String traceRequestId =
                settlementDetailQueryRepository.findLatestNonNoOpSettlementRequestId(settlementId);

        if (traceRequestId == null || traceRequestId.isBlank()) {
            throw new NotFoundException("trace entry not found");
        }

        return new SettlementTraceEntryResponse(traceRequestId);
    }
}