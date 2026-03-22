package com.settleops.domain.hold.api.dto;

import java.util.List;

public record AdminHoldQueueResponse(
        List<AdminHoldQueueRowDto> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}