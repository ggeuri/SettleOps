package com.settleops.domain.hold.infra;

import com.settleops.domain.hold.api.dto.AdminHoldQueueRowDto;
import com.settleops.domain.hold.api.dto.HoldQueueSearchRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HoldQueryRepository {
    Page<AdminHoldQueueRowDto> search(HoldQueueSearchRequestDto condition, Pageable pageable);
}