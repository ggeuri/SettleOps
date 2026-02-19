package com.settleops.domain.settlement.application;

import com.settleops.domain.settlement.dto.SettlementBatchRunResponse;
import com.settleops.domain.settlement.dto.SettlementPayActionResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class SettlementAdminCommandServiceImpl implements SettlementAdminCommandService {

    @Override
    public SettlementBatchRunResponse runBatch(LocalDate baseDate) {
        // TODO: A2 구현
        throw new UnsupportedOperationException("TODO: runBatch not implemented yet");
    }

    @Override
    public SettlementPayActionResponse requestPaid(String settlementId, String comment) {
        // TODO: request-paid 구현
        throw new UnsupportedOperationException("TODO: requestPaid not implemented yet");
    }

    @Override
    public SettlementPayActionResponse approvePaid(String settlementId, String comment) {
        // TODO: approve-paid 구현
        throw new UnsupportedOperationException("TODO: approvePaid not implemented yet");
    }
}