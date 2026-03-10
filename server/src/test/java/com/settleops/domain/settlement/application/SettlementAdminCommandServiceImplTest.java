package com.settleops.domain.settlement.application;

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
import com.settleops.global.audit.AuditLogger;
import com.settleops.global.enums.ReasonCode;
import com.settleops.global.error.BadRequestException;
import com.settleops.global.error.ConflictException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

        objectMapper = new ObjectMapper();

        service = new SettlementAdminCommandServiceImpl(
                settlementRepository,
                settlementBatchRepository,
                settlementLineRepository,
                paymentEventRepository,
                auditLogger,
                refundAdjustmentPolicy,
                settlementBatchRunRecorder,
                objectMapper
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
    void requestPaid_settlementNotFound_then404() {
        Mockito.when(settlementRepository.findById("S404")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requestPaid("S404", null, "req-test-001"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode().value()).isEqualTo(404);
                });
    }

    @Test
    void requestPaid_requestIdMissing_then400_BadRequest() {
        assertThatThrownBy(() -> service.requestPaid("S1", "memo", null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("requestId");
    }

    @Test
    void requestPaid_actorMissing_then401() {
        SecurityContextHolder.clearContext();

        Settlement settlement = Mockito.mock(Settlement.class);
        Mockito.when(settlement.isPayRequested()).thenReturn(true);
        Mockito.when(settlement.getSettlementId()).thenReturn("S1");
        Mockito.when(settlement.getMerchantId()).thenReturn("M1");
        Mockito.when(settlement.getPaidRequestedAt()).thenReturn(LocalDateTime.now());
        Mockito.when(settlement.getStatus()).thenReturn(SettlementStatus.PAY_REQUESTED);

        Mockito.when(settlementRepository.findById("S1")).thenReturn(Optional.of(settlement));

        assertThatThrownBy(() -> service.requestPaid("S1", "memo", "req-test-001"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode().value()).isEqualTo(401);
                });
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
    @DisplayName("runBatch 중 예외가 발생하면 FAIL과 UNEXPECTED_ERROR를 반환한다")
    void runBatch_unexpectedException_thenFailWithUnexpectedError() {
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

    }
}