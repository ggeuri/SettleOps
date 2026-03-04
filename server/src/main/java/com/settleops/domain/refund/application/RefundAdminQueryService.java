package com.settleops.domain.refund.application;

import com.settleops.domain.refund.api.dto.AdminRefundListItemDTO;
import com.settleops.domain.refund.domain.RefundStatus;
import com.settleops.domain.refund.infra.RefundReadRepository;
import com.settleops.global.error.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A6 운영 큐 조회(READ).
 * - status/from/to 옵션 파라미터를 "조회 조건"으로 정리해서 QueryDSL Repository로 전달한다.
 * - 기준 시각 SoT: refund.requestedAt (DB: requested_at)
 * - status는 enum(@Enumerated STRING)이라 enum으로 파싱 후 전달한다.
 */
@Service
@RequiredArgsConstructor
public class RefundAdminQueryService {

    private final RefundReadRepository refundReadRepository;

    public List<AdminRefundListItemDTO> list(String status, LocalDate from, LocalDate to) {
        RefundStatus parsedStatus = parseStatus(status);

        // 옵션 처리: from/to 중 하나만 오면 단일 날짜 조회로 맞춘다(예측 가능)
        if (from == null && to != null) from = to;
        if (to == null && from != null) to = from;

        LocalDateTime fromDt = (from == null) ? null : from.atStartOfDay();
        LocalDateTime toDt = (to == null) ? null : to.atTime(23, 59, 59, 999_999_999);

        return refundReadRepository.findAdminRefundQueue(parsedStatus, fromDt, toDt);
    }

    private RefundStatus parseStatus(String status) {
        if (status == null || status.isBlank()) return null;

        try {
            return RefundStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            // 파라미터/형식 오류는 400
            throw new BadRequestException("status is invalid");
        }
    }
}