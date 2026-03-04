package com.settleops.domain.settlement.application;

import com.settleops.domain.settlement.entity.SettlementBatch;
import com.settleops.domain.settlement.enums.SettlementBatchResult;
import com.settleops.domain.settlement.infra.SettlementBatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SettlementBatchRunRecorder { // settlement_batch 기록만 전담
    private final SettlementBatchRepository settlementBatchRepository;

    /**
     * @return start 성공 시 created row, 이미 존재하면 existing row (호출자가 SKIP 처리)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public StartResult start(LocalDate baseDate, String runId, String requestId, String actorId){
        try{
            SettlementBatch batch = SettlementBatch.completed(
              baseDate,
              runId,
              actorId,
                    SettlementBatchResult.OK,
                    requestId,
                    null,
                    null // finishedAt=null => 진행중
            );
            settlementBatchRepository.saveAndFlush(batch);
            return StartResult.created(batch);
        } catch (DataIntegrityViolationException e){
            SettlementBatch existing = settlementBatchRepository.findByBatchKey(baseDate)
                    .orElseThrow(()-> e);
            return StartResult.existing(existing);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeOk (String runId){
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

    public record StartResult(boolean created, SettlementBatch batch) {
        public static StartResult created(SettlementBatch b) {return new StartResult(true, b);}
        public static StartResult existing(SettlementBatch b) {return new StartResult(false, b);}
    }
}
