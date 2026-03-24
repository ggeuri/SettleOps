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

@Service
@RequiredArgsConstructor
public class RefundAdminQueryService {

    private final RefundReadRepository refundReadRepository;

    public Page<AdminRefundListItemDTO> list(
            String status,
            LocalDate from,
            LocalDate to,
            String settlementId,
            String keyword,
            String sortKey,
            String sortDirection,
            Pageable pageable
    ) {
        RefundStatus parsedStatus = parseStatus(status);

        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("from must be <= to");
        }

        String normalizedSettlementId = settlementId == null ? null : settlementId.trim();
        String normalizedKeyword = keyword == null ? null : keyword.trim();

        if (normalizedSettlementId != null && normalizedSettlementId.isEmpty()) {
            throw new BadRequestException("settlementId is invalid");
        }

        LocalDateTime fromDt = (from == null) ? null : from.atStartOfDay();
        LocalDateTime toDt = (to == null) ? null : to.atTime(23, 59, 59, 999_999_000);

        return refundReadRepository.findAdminRefundQueue(
                parsedStatus,
                fromDt,
                toDt,
                normalizedSettlementId,
                normalizedKeyword,
                sortKey,
                sortDirection,
                pageable
        );
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