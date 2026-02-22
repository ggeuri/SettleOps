package com.settleops.domain.settlement.application;

import com.settleops.domain.settlement.dto.SettlementBatchRunResponse;
import com.settleops.domain.settlement.dto.SettlementPayActionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Service
public class SettlementAdminCommandServiceImpl implements SettlementAdminCommandService {

    @Override
    public SettlementBatchRunResponse runBatch(LocalDate baseDate) {
        // TODO: A2 구현
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "배치 실행 기능은 다음 PR에서 구현됩니다.");
    }

    @Override
    public SettlementPayActionResponse requestPaid(String settlementId, String comment) {
        // TODO: request-paid 구현
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "지급 요청 기능은 다음 PR에서 구현됩니다.");
    }

    @Override
    public SettlementPayActionResponse approvePaid(String settlementId, String comment) {
        // TODO: approve-paid 구현
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "지급 승인 기능은 다음 PR에서 구현됩니다.");
    }
}