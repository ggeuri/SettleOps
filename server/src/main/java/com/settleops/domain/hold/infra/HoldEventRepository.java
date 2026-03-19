package com.settleops.domain.hold.infra;

import com.settleops.domain.hold.domain.HoldEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HoldEventRepository extends JpaRepository<HoldEvent, Long> {
}