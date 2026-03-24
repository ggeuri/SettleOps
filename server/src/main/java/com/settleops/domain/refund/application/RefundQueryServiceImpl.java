package com.settleops.domain.refund.application;

import com.settleops.domain.refund.api.dto.RefundRowDTO;
import com.settleops.domain.refund.domain.RefundStatus;
import com.settleops.domain.refund.infra.RefundReadRepository;
import com.settleops.global.error.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RefundQueryServiceImpl implements RefundQueryService {

    private final RefundReadRepository refundReadRepository;

    @Override
    public Page<RefundRowDTO> myRefunds(
            String loginMerchantId,
            String status,
            LocalDate from,
            LocalDate to,
            String keyword,
            String sortKey,
            String sortDirection,
            Pageable pageable
    ) {
        RefundStatus refundStatus = parseStatus(status);

        LocalDateTime fromDateTime = from == null ? null : from.atStartOfDay();
        LocalDateTime toDateTime = to == null ? null : to.plusDays(1).atStartOfDay().minusNanos(1);

        return refundReadRepository.findMyRefunds(
                loginMerchantId,
                refundStatus,
                fromDateTime,
                toDateTime,
                keyword,
                sortKey,
                sortDirection,
                pageable
        );
    }

    private RefundStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        String normalized = status.trim().toUpperCase();

        if ("ALL".equals(normalized)) {
            return null;
        }

        try {
            return RefundStatus.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("invalid refund status: " + status);
        }
    }
}