package com.settleops.domain.refund.application;

import com.settleops.domain.refund.application.dto.ApprovedRefundAdjustment;
import com.settleops.domain.refund.infra.RefundSettlementReadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RefundSettlementAssemblerImpl implements RefundSettlementAssembler {

    private final RefundSettlementReadRepository refundSettlementReadRepository;

    @Override
    public List<ApprovedRefundAdjustment> getApprovedRefundAdjustmentsForBaseDate(LocalDate baseDate) {
        // baseDate 당일 00:00 이전에 승인된 건만 조회
        LocalDateTime cutoffExclusive = baseDate.atStartOfDay();

        return refundSettlementReadRepository
                .findApprovedRefundsWithoutSettlementLinkBefore(cutoffExclusive);
    }
}