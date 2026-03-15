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
 * - 다음 배치에 반영할 REFUND line 입력집합을 산출한다.
 *
 * LOCKED:
 * - refund.status = APPROVED
 * - decidedAt 존재
 * - refund_settlement_link 미존재(미반영)
 * - baseDate(KST) 당일 00:00 이전 승인 완료건만 포함
 *
 * 비책임:
 * - settlement 생성
 * - settlement_line(REFUND) insert
 * - refund_settlement_link insert
 * - batch 내부 재판정
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

        // "다음 배치" 규칙:
        // approval 당일이 아니라 그 다음 baseDate 배치에서 반영되도록
        // baseDate 당일 00:00 이전에 승인된 건만 조회한다.
        LocalDateTime cutoffExclusive = baseDate.atStartOfDay();

        return refundSettlementReadRepository.findApprovedUnlinkedRefundAdjustmentsBefore(cutoffExclusive);
    }
}