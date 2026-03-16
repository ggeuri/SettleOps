package com.settleops.domain.refund.application;

import com.settleops.domain.refund.application.dto.ApprovedRefundAdjustment;
import com.settleops.domain.refund.infra.RefundSettlementReadRepository;
import com.settleops.global.error.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * C 오너 구현체.
 *
 * 책임:
 * - 다음 배치 REFUND line 입력집합 산출
 *
 * LOCKED:
 * - 날짜 경계는 KST(Asia/Seoul) 기준 baseDate 당일 00:00
 * - 포함 대상은 decidedAt < baseDate.atStartOfDay() 인 APPROVED refund
 * - refund_settlement_link가 없는 건만 포함
 *
 * 비책임:
 * - settlement_line(REFUND) insert
 * - refund_settlement_link insert
 * - request-paid 차단/해제 판정
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefundSettlementAssemblerImpl implements RefundSettlementAssembler {

    private final RefundSettlementReadRepository refundSettlementReadRepository;

    @Override
    public List<ApprovedRefundAdjustment> getApprovedRefundAdjustmentsForBaseDate(LocalDate baseDate) {
        if (baseDate == null) {
            throw new BadRequestException("baseDate is required");
        }

        // LOCKED:
        // baseDate 경계는 KST(Asia/Seoul) 당일 00:00 고정.
        // 다음 배치 반영 규칙에 따라 decidedAt < cutoff 인 승인건만 포함한다.
        LocalDateTime cutoffExclusive = baseDate.atStartOfDay();

        return refundSettlementReadRepository.findApprovedUnlinkedRefundAdjustmentsBefore(cutoffExclusive);
    }
}