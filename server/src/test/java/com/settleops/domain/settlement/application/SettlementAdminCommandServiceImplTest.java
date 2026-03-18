package com.settleops.domain.settlement.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.settleops.domain.payment.infra.PaymentEventRepository;
import com.settleops.domain.settlement.dto.SettlementBatchRunResponse;
import com.settleops.domain.settlement.entity.Settlement;
import com.settleops.domain.settlement.entity.SettlementBatch;
import com.settleops.domain.settlement.enums.SettlementBatchResult;
import com.settleops.domain.settlement.enums.SettlementStatus;
import com.settleops.domain.settlement.infra.SettlementBatchRepository;
import com.settleops.domain.settlement.infra.SettlementLineRepository;
import com.settleops.domain.settlement.infra.SettlementRepository;
import com.settleops.global.audit.ActorType;
import com.settleops.global.audit.AuditLogCommand;
import com.settleops.global.audit.AuditLogger;
import com.settleops.global.audit.EntityType;
import com.settleops.global.enums.Action;
import com.settleops.global.enums.ReasonCode;
import com.settleops.global.error.AuditableConflictException;
import com.settleops.global.error.BadRequestException;
import com.settleops.global.error.ConflictException;
import com.settleops.global.error.NotFoundException;
import com.settleops.global.error.UnauthorizedException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class SettlementAdminCommandServiceImplTest {

    private SettlementRepository settlementRepository;
    private SettlementBatchRepository settlementBatchRepository;
    private SettlementLineRepository settlementLineRepository;
    private PaymentEventRepository paymentEventRepository;

    private AuditLogger auditLogger;
    private RefundAdjustmentPolicy refundAdjustmentPolicy;
    private SettlementBatchRunRecorder settlementBatchRunRecorder;
    private EntityManager entityManager;

    private ObjectMapper objectMapper;

    private SettlementAdminCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        settlementRepository = Mockito.mock(SettlementRepository.class);
        settlementBatchRepository = Mockito.mock(SettlementBatchRepository.class);
        settlementLineRepository = Mockito.mock(SettlementLineRepository.class);
        paymentEventRepository = Mockito.mock(PaymentEventRepository.class);

        auditLogger = Mockito.mock(AuditLogger.class);
        refundAdjustmentPolicy = Mockito.mock(RefundAdjustmentPolicy.class);
        settlementBatchRunRecorder = Mockito.mock(SettlementBatchRunRecorder.class);
        entityManager = Mockito.mock(EntityManager.class);

        objectMapper = new ObjectMapper();

        service = new SettlementAdminCommandServiceImpl(
                settlementRepository,
                settlementBatchRepository,
                settlementLineRepository,
                paymentEventRepository,
                auditLogger,
                refundAdjustmentPolicy,
                settlementBatchRunRecorder,
                objectMapper,
                entityManager
        );

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin1", "N/A")
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requestPaid_refundAdjustmentPending_true_then409_REFUND_ADJUSTMENT_PENDING() {
        Settlement settlement = Mockito.mock(Settlement.class);

        Mockito.when(settlement.getSettlementId()).thenReturn("S1");
        Mockito.when(settlement.getMerchantId()).thenReturn("M1");
        Mockito.when(settlement.getBatchId()).thenReturn(1L);
        Mockito.when(settlement.getStatus()).thenReturn(SettlementStatus.READY);

        Mockito.when(settlement.isPayRequested()).thenReturn(false);
        Mockito.when(settlement.isHoldActive()).thenReturn(false);
        Mockito.when(settlement.isReady()).thenReturn(true);

        Mockito.when(settlementRepository.findById("S1")).thenReturn(Optional.of(settlement));

        SettlementBatch batch = Mockito.mock(SettlementBatch.class);
        Mockito.when(batch.getFinishedAt()).thenReturn(LocalDateTime.now());
        Mockito.when(batch.getResult()).thenReturn(SettlementBatchResult.OK);
        Mockito.when(settlementBatchRepository.findById(1L)).thenReturn(Optional.of(batch));

        Mockito.when(refundAdjustmentPolicy.isRefundAdjustmentPending("S1")).thenReturn(true);

        assertThatThrownBy(() -> service.requestPaid("S1", "memo", "req-test-001"))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> {
                    ConflictException ce = (ConflictException) ex;
                    assertThat(ce.toErrorResponse().reason())
                            .isEqualTo(ReasonCode.REFUND_ADJUSTMENT_PENDING.name());
                });

        verifyNoInteractions(auditLogger);
        verify(settlementBatchRepository, times(1)).findById(1L);
        verify(refundAdjustmentPolicy, times(1)).isRefundAdjustmentPending("S1");
    }

    @Test
    void requestPaid_requestIdMissing_then400_BadRequest() {
        assertThatThrownBy(() -> service.requestPaid("S1", "memo", null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("requestId");
    }

    @Test
    void requestPaid_settlementNotFound_then404() {
        Mockito.when(settlementRepository.findById("S404")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requestPaid("S404", null, "req-test-001"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("settlement not found");
    }

    @Test
    void requestPaid_actorMissing_then401() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> service.requestPaid("S1", "memo", "req-test-001"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("unauthorized");
    }

    @Test
    void requestPaid_settlementIdBlank_then400() {
        assertThatThrownBy(() -> service.requestPaid("   ", null, "req-test-001"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void requestPaid_holdActive_then409_HOLD_ACTIVE_even_if_not_ready() {
        Settlement settlement = Mockito.mock(Settlement.class);

        Mockito.when(settlement.getSettlementId()).thenReturn("S1");
        Mockito.when(settlement.getMerchantId()).thenReturn("M1");

        Mockito.when(settlement.isPayRequested()).thenReturn(false);
        Mockito.when(settlement.isHoldActive()).thenReturn(true);

        Mockito.when(settlementRepository.findById("S1")).thenReturn(Optional.of(settlement));

        assertThatThrownBy(() -> service.requestPaid("S1", "memo", "req-test-001"))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> {
                    ConflictException ce = (ConflictException) ex;
                    assertThat(ce.toErrorResponse().reason())
                            .isEqualTo(ReasonCode.HOLD_ACTIVE.name());
                });

        verifyNoInteractions(refundAdjustmentPolicy);
        verifyNoInteractions(settlementBatchRepository);
        verifyNoInteractions(auditLogger);
    }

    @Test
    void requestPaid_notReady_then409_SETTLEMENT_NOT_READY() {
        Settlement settlement = Mockito.mock(Settlement.class);

        Mockito.when(settlement.getSettlementId()).thenReturn("S1");
        Mockito.when(settlement.getMerchantId()).thenReturn("M1");
        Mockito.when(settlement.getStatus()).thenReturn(SettlementStatus.PAY_REQUESTED);

        Mockito.when(settlement.isPayRequested()).thenReturn(false);
        Mockito.when(settlement.isHoldActive()).thenReturn(false);
        Mockito.when(settlement.isReady()).thenReturn(false);

        Mockito.when(settlementRepository.findById("S1")).thenReturn(Optional.of(settlement));

        assertThatThrownBy(() -> service.requestPaid("S1", "memo", "req-test-001"))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> {
                    ConflictException ce = (ConflictException) ex;
                    assertThat(ce.toErrorResponse().reason())
                            .isEqualTo(ReasonCode.SETTLEMENT_NOT_READY.name());
                });

        verifyNoInteractions(refundAdjustmentPolicy);
        verifyNoInteractions(settlementBatchRepository);
        verifyNoInteractions(auditLogger);
    }

    @Test
    void requestPaid_alreadyPayRequested_then200_and_auditContainsNoOpMeta() throws Exception {
        Settlement settlement = Mockito.mock(Settlement.class);

        Mockito.when(settlement.getSettlementId()).thenReturn("S1");
        Mockito.when(settlement.getMerchantId()).thenReturn("M1");
        Mockito.when(settlement.getStatus()).thenReturn(SettlementStatus.PAY_REQUESTED);
        Mockito.when(settlement.getPaidRequestedAt()).thenReturn(LocalDateTime.of(2026, 3, 16, 10, 0));
        Mockito.when(settlement.isPayRequested()).thenReturn(true);

        Mockito.when(settlementRepository.findById("S1")).thenReturn(Optional.of(settlement));

        var response = service.requestPaid("S1", "memo", "req-test-001");

        assertThat(response.status()).isEqualTo(SettlementStatus.PAY_REQUESTED);

        ArgumentCaptor<AuditLogCommand> captor = ArgumentCaptor.forClass(AuditLogCommand.class);
        verify(auditLogger).log(captor.capture());

        JsonNode meta = objectMapper.readTree(captor.getValue().getMetaJson());
        assertThat(meta.get("noOp").asBoolean()).isTrue();
        assertThat(meta.get("noOpReason").asText()).isEqualTo("ALREADY_PAY_REQUESTED");
        assertThat(meta.get("comment").asText()).isEqualTo("memo");
    }

    @Test
    void approvePaid_alreadyPaid_then200_and_auditContainsNoOpMeta() throws Exception {
        Settlement settlement = Mockito.mock(Settlement.class);

        Mockito.when(settlement.getSettlementId()).thenReturn("S1");
        Mockito.when(settlement.getMerchantId()).thenReturn("M1");
        Mockito.when(settlement.getStatus()).thenReturn(SettlementStatus.PAID);
        Mockito.when(settlement.getPaidApprovedAt()).thenReturn(LocalDateTime.of(2026, 3, 16, 11, 0));
        Mockito.when(settlement.isPaid()).thenReturn(true);

        Mockito.when(settlementRepository.findById("S1")).thenReturn(Optional.of(settlement));

        var response = service.approvePaid("S1", "memo", "req-test-001");

        assertThat(response.status()).isEqualTo(SettlementStatus.PAID);

        ArgumentCaptor<AuditLogCommand> captor = ArgumentCaptor.forClass(AuditLogCommand.class);
        verify(auditLogger).log(captor.capture());

        JsonNode meta = objectMapper.readTree(captor.getValue().getMetaJson());
        assertThat(meta.get("noOp").asBoolean()).isTrue();
        assertThat(meta.get("noOpReason").asText()).isEqualTo("ALREADY_PAID");
        assertThat(meta.get("comment").asText()).isEqualTo("memo");

        verifyNoInteractions(entityManager);
    }

    @Test
    @DisplayName("runBatch 중 예외가 발생하면 FAIL과 UNEXPECTED_ERROR를 반환한다")
    void runBatch_unexpectedException_thenFailWithUnexpectedError() throws Exception {
        LocalDate baseDate = LocalDate.of(2026, 3, 2);

        SettlementBatch startedBatch = Mockito.mock(SettlementBatch.class);
        Mockito.when(startedBatch.getBatchId()).thenReturn(1L);

        Mockito.when(settlementBatchRunRecorder.startOrThrow(
                Mockito.eq(baseDate),
                Mockito.anyString(),
                Mockito.eq("req-test-001"),
                Mockito.anyString()
        )).thenReturn(startedBatch);

        Mockito.when(paymentEventRepository.findConfirmedPaymentsByOccurredAtRange(
                Mockito.any(LocalDateTime.class),
                Mockito.any(LocalDateTime.class)
        )).thenThrow(new RuntimeException("boom"));

        SettlementBatch failedBatch = Mockito.mock(SettlementBatch.class);
        Mockito.when(failedBatch.getBatchKey()).thenReturn(baseDate);
        Mockito.when(failedBatch.getRunId()).thenReturn("run-001");
        Mockito.when(failedBatch.getFailReason()).thenReturn("UNEXPECTED_ERROR");
        Mockito.when(failedBatch.getCreatedAt()).thenReturn(LocalDateTime.now().minusMinutes(1));
        Mockito.when(failedBatch.getFinishedAt()).thenReturn(LocalDateTime.now());

        Mockito.when(settlementBatchRepository.findByRunId(Mockito.anyString()))
                .thenReturn(Optional.of(failedBatch));

        SettlementBatchRunResponse response = service.runBatch(baseDate, "req-test-001");

        assertThat(response.result()).isEqualTo(SettlementBatchRunResponse.RunResult.FAIL);
        assertThat(response.failReason()).isEqualTo("UNEXPECTED_ERROR");
        assertThat(response.finishedAt()).isNotNull();

        verify(settlementBatchRunRecorder, times(1))
                .completeFail(Mockito.anyString(), Mockito.eq("UNEXPECTED_ERROR"));
        Mockito.verify(settlementBatchRunRecorder, Mockito.never()).completeOk(Mockito.anyString());

        ArgumentCaptor<AuditLogCommand> auditCaptor = ArgumentCaptor.forClass(AuditLogCommand.class);
        verify(auditLogger, Mockito.atLeastOnce()).log(auditCaptor.capture());

        List<AuditLogCommand> auditCommands = auditCaptor.getAllValues();

        AuditLogCommand completedAudit = auditCommands.stream()
                .filter(cmd -> cmd.getAction() == Action.BATCH_RUN_COMPLETED)
                .findFirst()
                .orElseThrow(() -> new AssertionError("BATCH_RUN_COMPLETED audit not found"));

        JsonNode meta = objectMapper.readTree(completedAudit.getMetaJson());

        assertThat(meta.get("result").asText()).isEqualTo("FAIL");
        assertThat(meta.get("failReason").asText()).isEqualTo("UNEXPECTED_ERROR");
        assertThat(meta.get("errorType").asText()).isEqualTo("RuntimeException");
    }

    @Test
    void approvePaid_sameApprover_thenAuditableConflictException() {
        Settlement settlement = Mockito.mock(Settlement.class);

        Mockito.when(settlement.getSettlementId()).thenReturn("S1");
        Mockito.when(settlement.getMerchantId()).thenReturn("M1");
        Mockito.when(settlement.getStatus()).thenReturn(SettlementStatus.PAY_REQUESTED);

        Mockito.when(settlement.isPaid()).thenReturn(false);
        Mockito.when(settlement.isPayRequested()).thenReturn(true);
        Mockito.when(settlement.violatesFourEyes("admin1")).thenReturn(true);

        Mockito.when(settlementRepository.findById("S1")).thenReturn(Optional.of(settlement));
        Mockito.when(settlementRepository.findByIdForUpdate("S1")).thenReturn(Optional.of(settlement));

        assertThatThrownBy(() -> service.approvePaid("S1", "memo", "req-test-001"))
                .isInstanceOf(AuditableConflictException.class)
                .satisfies(ex -> {
                    AuditableConflictException ace = (AuditableConflictException) ex;
                    assertThat(ace.getReasonCode()).isEqualTo(ReasonCode.SAME_APPROVER_NOT_ALLOWED);
                    assertThat(ace.getActorType()).isEqualTo(ActorType.ADMIN);
                    assertThat(ace.getActorId()).isEqualTo("admin1");
                    assertThat(ace.getAction()).isEqualTo(Action.SETTLEMENT_PAY_APPROVED);
                    assertThat(ace.getEntityType()).isEqualTo(EntityType.SETTLEMENT);
                    assertThat(ace.getEntityId()).isEqualTo("S1");
                    assertThat(ace.getMerchantId()).isEqualTo("M1");
                    assertThat(ace.getStatusBefore()).isEqualTo(SettlementStatus.PAY_REQUESTED.name());
                    assertThat(ace.getStatusAfter()).isEqualTo(SettlementStatus.PAY_REQUESTED.name());
                    assertThat(ace.getComment()).isEqualTo("memo");
                });

        verify(entityManager).detach(settlement);
        verifyNoInteractions(auditLogger);
    }

    @Test
    void requestPaid_batchFailed_then409_BATCH_FAILED() {
        Settlement settlement = Mockito.mock(Settlement.class);

        Mockito.when(settlement.getSettlementId()).thenReturn("S1");
        Mockito.when(settlement.getMerchantId()).thenReturn("M1");
        Mockito.when(settlement.getBatchId()).thenReturn(1L);
        Mockito.when(settlement.getStatus()).thenReturn(SettlementStatus.READY);

        Mockito.when(settlement.isPayRequested()).thenReturn(false);
        Mockito.when(settlement.isHoldActive()).thenReturn(false);
        Mockito.when(settlement.isReady()).thenReturn(true);

        Mockito.when(settlementRepository.findById("S1")).thenReturn(Optional.of(settlement));

        SettlementBatch batch = Mockito.mock(SettlementBatch.class);
        Mockito.when(batch.getFinishedAt()).thenReturn(LocalDateTime.now());
        Mockito.when(batch.getResult()).thenReturn(SettlementBatchResult.FAIL);
        Mockito.when(settlementBatchRepository.findById(1L)).thenReturn(Optional.of(batch));

        assertThatThrownBy(() -> service.requestPaid("S1", "memo", "req-test-001"))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> {
                    ConflictException ce = (ConflictException) ex;
                    assertThat(ce.toErrorResponse().reason())
                            .isEqualTo(ReasonCode.BATCH_FAILED.name());
                });

        verify(settlementBatchRepository, times(1)).findById(1L);
        verifyNoInteractions(refundAdjustmentPolicy);
        verifyNoInteractions(auditLogger);
    }

    @Test
    void requestPaid_notReady_then409_SETTLEMENT_NOT_READY_even_if_batchFailed() {
        Settlement settlement = Mockito.mock(Settlement.class);

        Mockito.when(settlement.getSettlementId()).thenReturn("S1");
        Mockito.when(settlement.getMerchantId()).thenReturn("M1");
        Mockito.when(settlement.getBatchId()).thenReturn(1L);
        Mockito.when(settlement.getStatus()).thenReturn(SettlementStatus.PAY_REQUESTED);

        Mockito.when(settlement.isPayRequested()).thenReturn(false);
        Mockito.when(settlement.isHoldActive()).thenReturn(false);
        Mockito.when(settlement.isReady()).thenReturn(false);

        Mockito.when(settlementRepository.findById("S1")).thenReturn(Optional.of(settlement));

        assertThatThrownBy(() -> service.requestPaid("S1", "memo", "req-test-001"))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> {
                    ConflictException ce = (ConflictException) ex;
                    assertThat(ce.toErrorResponse().reason())
                            .isEqualTo(ReasonCode.SETTLEMENT_NOT_READY.name());
                });

        verifyNoInteractions(settlementBatchRepository);
        verifyNoInteractions(refundAdjustmentPolicy);
        verifyNoInteractions(auditLogger);
    }

    @Test
    void requestPaid_batchFailed_then409_BATCH_FAILED_even_if_refundAdjustmentPending() {
        Settlement settlement = Mockito.mock(Settlement.class);

        Mockito.when(settlement.getSettlementId()).thenReturn("S1");
        Mockito.when(settlement.getMerchantId()).thenReturn("M1");
        Mockito.when(settlement.getBatchId()).thenReturn(1L);
        Mockito.when(settlement.getStatus()).thenReturn(SettlementStatus.READY);

        Mockito.when(settlement.isPayRequested()).thenReturn(false);
        Mockito.when(settlement.isHoldActive()).thenReturn(false);
        Mockito.when(settlement.isReady()).thenReturn(true);

        Mockito.when(settlementRepository.findById("S1")).thenReturn(Optional.of(settlement));

        SettlementBatch batch = Mockito.mock(SettlementBatch.class);
        Mockito.when(batch.getFinishedAt()).thenReturn(LocalDateTime.now());
        Mockito.when(batch.getResult()).thenReturn(SettlementBatchResult.FAIL);
        Mockito.when(settlementBatchRepository.findById(1L)).thenReturn(Optional.of(batch));

        assertThatThrownBy(() -> service.requestPaid("S1", "memo", "req-test-001"))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> {
                    ConflictException ce = (ConflictException) ex;
                    assertThat(ce.toErrorResponse().reason())
                            .isEqualTo(ReasonCode.BATCH_FAILED.name());
                });

        verify(settlementBatchRepository, times(1)).findById(1L);
        verifyNoInteractions(refundAdjustmentPolicy);
        verifyNoInteractions(auditLogger);
    }
}