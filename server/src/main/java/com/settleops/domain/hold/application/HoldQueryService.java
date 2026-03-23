package com.settleops.domain.hold.application;

import com.settleops.domain.hold.api.dto.AdminHoldQueueResponse;
import com.settleops.domain.hold.api.dto.AdminHoldQueueRowDto;
import com.settleops.domain.hold.api.dto.HoldQueueSearchRequestDto;
import com.settleops.domain.hold.infra.HoldQueryRepository;
import com.settleops.global.error.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HoldQueryService {

    private final HoldQueryRepository holdQueryRepository;

    public AdminHoldQueueResponse search(HoldQueueSearchRequestDto request, Pageable pageable) {
        if (request == null) {
            throw new BadRequestException("요청 값이 올바르지 않습니다.");
        }

        validate(request);

        Page<AdminHoldQueueRowDto> pageResult = holdQueryRepository.search(request, pageable);

        return new AdminHoldQueueResponse(
                pageResult.getContent(),
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages()
        );
    }

    private void validate(HoldQueueSearchRequestDto request) {
        if (request == null) {
            throw new BadRequestException("요청 값이 올바르지 않습니다.");
        }

        if (request.settlementId() != null && request.settlementId().isBlank()) {
            throw new BadRequestException("settlementId 값이 올바르지 않습니다.");
        }

        if (request.merchantId() != null && request.merchantId().isBlank()) {
            throw new BadRequestException("merchantId 값이 올바르지 않습니다.");
        }
    }
}