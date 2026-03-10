package com.settleops.domain.settlement.application;

import com.settleops.domain.settlement.entity.SettlementBatch;
import com.settleops.domain.settlement.infra.SettlementBatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class SettlementBatchRunRecorder {

    private final SettlementBatchRepository settlementBatchRepository;

    /**
     * 배치 "시작" 레코드 생성 (멱등은 호출자(Service)가 catch로 처리)
     * - REQUIRES_NEW: UNIQUE 충돌이 상위 트랜잭션(runBatch)을 rollback-only로 만들지 않도록 격리
     * - saveAndFlush: 여기서 batch_id 확정 + UNIQUE 충돌 즉시 발생
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SettlementBatch startOrThrow(LocalDate baseDate, String runId, String requestId, String triggeredBy) {
        SettlementBatch batch = SettlementBatch.started(baseDate, runId, triggeredBy, requestId);
        return settlementBatchRepository.saveAndFlush(batch);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeOk(String runId) {
        SettlementBatch batch = settlementBatchRepository.findByRunId(runId)
                .orElseThrow(() -> new IllegalStateException("batch not found by runId=" + runId));

        batch.markOk(LocalDateTime.now());
        settlementBatchRepository.save(batch);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeFail(String runId, String failReason) {
        SettlementBatch batch = settlementBatchRepository.findByRunId(runId)
                .orElseThrow(() -> new IllegalStateException("batch not found by runId=" + runId));

        batch.markFail(failReason, LocalDateTime.now());
        settlementBatchRepository.save(batch);
    }
}