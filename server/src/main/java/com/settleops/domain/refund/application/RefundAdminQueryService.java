package com.settleops.domain.refund.application;

import com.settleops.domain.refund.api.dto.AdminRefundListItemDTO;
import com.settleops.domain.refund.domain.RefundStatus;
import com.settleops.domain.refund.infra.RefundReadRepository;
import com.settleops.global.error.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

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

    public Page<AdminRefundListItemDTO> list(
            String status,
            LocalDate from,
            LocalDate to,
            Pageable pageable
    ) {
        RefundStatus parsedStatus = parseStatus(status);

        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("from must be <= to");
        }

        LocalDateTime fromDt = (from == null) ? null : from.atStartOfDay();
        LocalDateTime toDt = (to == null) ? null : to.atTime(23, 59, 59, 999_999_000);

        return refundReadRepository.findAdminRefundQueue(parsedStatus, fromDt, toDt, pageable);
    }

    private RefundStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        try {
            return RefundStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("status is invalid");
        }
    }
}