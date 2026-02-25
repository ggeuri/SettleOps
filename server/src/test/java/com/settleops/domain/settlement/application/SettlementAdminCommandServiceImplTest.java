package com.settleops.domain.settlement.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.settleops.domain.settlement.entity.Settlement;
import com.settleops.domain.settlement.entity.SettlementBatch;
import com.settleops.domain.settlement.enums.SettlementBatchResult;
import com.settleops.domain.settlement.enums.SettlementStatus;
import com.settleops.domain.settlement.infra.SettlementBatchRepository;
import com.settleops.domain.settlement.infra.SettlementRepository;
import com.settleops.global.audit.AuditLogger;
import com.settleops.global.enums.ReasonCode;
import com.settleops.global.error.BadRequestException;
import com.settleops.global.error.ConflictException;
import com.settleops.global.logging.RequestIdKeys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.MDC;
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
    private AuditLogger auditLogger;
    private RefundAdjustmentPolicy refundAdjustmentPolicy;
    private SettlementBatchRunRecorder settlementBatchRunRecorder;
    private ObjectMapper objectMapper;

    private SettlementAdminCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        settlementRepository = Mockito.mock(SettlementRepository.class);
        settlementBatchRepository = Mockito.mock(SettlementBatchRepository.class);
        auditLogger = Mockito.mock(AuditLogger.class);
        refundAdjustmentPolicy = Mockito.mock(RefundAdjustmentPolicy.class);
        settlementBatchRunRecorder = Mockito.mock(SettlementBatchRunRecorder.class);
        objectMapper = new ObjectMapper();

        service = new SettlementAdminCommandServiceImpl(
                settlementRepository,
                settlementBatchRepository,
                auditLogger,
                refundAdjustmentPolicy,
                settlementBatchRunRecorder,
                objectMapper
        );

        MDC.put(RequestIdKeys.MDC_KEY, "test-request-id");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin1", "N/A")
        );
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void requestPaid_refundAdjustmentPending_true_then409_REFUND_ADJUSTMENT_PENDING() {
        Settlement settlement = Mockito.mock(Settlement.class);
        LocalDate baseDate = LocalDate.of(2026, 3, 4);

        Mockito.when(settlement.getSettlementId()).thenReturn("S1");
        Mockito.when(settlement.getMerchantId()).thenReturn("M1");
        Mockito.when(settlement.getBaseDate()).thenReturn(baseDate);
        Mockito.when(settlement.getStatus()).thenReturn(SettlementStatus.READY); // 테스트 안정성(미래 방어)

        Mockito.when(settlement.isPayRequested()).thenReturn(false);
        Mockito.when(settlement.isHoldActive()).thenReturn(false);
        Mockito.when(settlement.isReady()).thenReturn(true);

        Mockito.when(settlementRepository.findById("S1")).thenReturn(Optional.of(settlement));

        // 서비스 isBatchFailed(baseDate) 정합: findByBatchKey(baseDate)로 스텁
        SettlementBatch batch = Mockito.mock(SettlementBatch.class);
        Mockito.when(batch.getFinishedAt()).thenReturn(LocalDateTime.now()); // 완료된 배치만 판정
        Mockito.when(batch.getResult()).thenReturn(SettlementBatchResult.OK); // FAIL 아님
        Mockito.when(settlementBatchRepository.findByBatchKey(baseDate)).thenReturn(Optional.of(batch));

        Mockito.when(refundAdjustmentPolicy.isRefundAdjustmentPending("S1")).thenReturn(true);

        assertThatThrownBy(() -> service.requestPaid("S1", "memo"))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> {
                    ConflictException ce = (ConflictException) ex;
                    assertThat(ce.toErrorResponse().reason())
                            .isEqualTo(ReasonCode.REFUND_ADJUSTMENT_PENDING.name());
                });

        verifyNoInteractions(auditLogger);
        verify(settlementBatchRepository, times(1)).findByBatchKey(baseDate);
        verify(refundAdjustmentPolicy, times(1)).isRefundAdjustmentPending("S1");
        Mockito.verifyNoMoreInteractions(refundAdjustmentPolicy);
    }

    @Test
    void requestPaid_settlementNotFound_then404() {
        Mockito.when(settlementRepository.findById("S404")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requestPaid("S404", null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode().value()).isEqualTo(404);
                });
    }

    @Test
    void requestPaid_requestIdMissing_then400_BadRequest() {
        MDC.clear();

        Settlement settlement = Mockito.mock(Settlement.class);
        Mockito.when(settlement.isPayRequested()).thenReturn(true);

        Mockito.when(settlement.getSettlementId()).thenReturn("S1");
        Mockito.when(settlement.getMerchantId()).thenReturn("M1");
        Mockito.when(settlement.getPaidRequestedAt()).thenReturn(LocalDateTime.now());

        Mockito.when(settlementRepository.findById("S1")).thenReturn(Optional.of(settlement));

        assertThatThrownBy(() -> service.requestPaid("S1", "memo"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("requestId");
    }

    @Test
    void requestPaid_actorMissing_then401() {
        SecurityContextHolder.clearContext(); // 인증 제거

        Settlement settlement = Mockito.mock(Settlement.class);
        Mockito.when(settlement.isPayRequested()).thenReturn(true);
        Mockito.when(settlement.getSettlementId()).thenReturn("S1");
        Mockito.when(settlement.getMerchantId()).thenReturn("M1");
        Mockito.when(settlement.getPaidRequestedAt()).thenReturn(LocalDateTime.now());

        Mockito.when(settlementRepository.findById("S1")).thenReturn(Optional.of(settlement));

        assertThatThrownBy(() -> service.requestPaid("S1", "memo"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode().value()).isEqualTo(401);
                });
    }

    @Test
    void requestPaid_settlementIdBlank_then400() {
        assertThatThrownBy(() -> service.requestPaid("   ", null))
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

        assertThatThrownBy(() -> service.requestPaid("S1", "memo"))
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

        assertThatThrownBy(() -> service.requestPaid("S1", "memo"))
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
}