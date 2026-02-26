package com.settleops.domain.settlement.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.settleops.domain.settlement.entity.Settlement;
import com.settleops.domain.settlement.enums.SettlementBatchResult;
import com.settleops.domain.settlement.enums.SettlementStatus;
import com.settleops.domain.settlement.infra.SettlementBatchRepository;
import com.settleops.domain.settlement.infra.SettlementRepository;
import com.settleops.global.audit.AuditLogCommand;
import com.settleops.global.audit.AuditLogger;
import com.settleops.global.enums.ReasonCode;
import com.settleops.global.error.ConflictException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class SettlementAdminCommandServiceImplTest {

    private final SettlementRepository settlementRepository = Mockito.mock(SettlementRepository.class);
    private final SettlementBatchRepository settlementBatchRepository = Mockito.mock(SettlementBatchRepository.class);
    private final AuditLogger auditLogger = Mockito.mock(AuditLogger.class);
    private final RefundAdjustmentPolicy refundAdjustmentPolicy = Mockito.mock(RefundAdjustmentPolicy.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final SettlementAdminCommandServiceImpl service =
            new SettlementAdminCommandServiceImpl(
                    settlementRepository,
                    settlementBatchRepository,
                    auditLogger,
                    refundAdjustmentPolicy,
                    objectMapper
            );

    @AfterEach
    void tearDown() {
        MDC.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void requestPaid_refundAdjustmentPending_true_then409_REFUND_ADJUSTMENT_PENDING() {
        MDC.put("requestId", "test-request-id");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin1", "N/A")
        );

        Settlement settlement = Mockito.mock(Settlement.class);
        Mockito.when(settlement.getSettlementId()).thenReturn("S1");
        Mockito.when(settlement.getMerchantId()).thenReturn("M1");
        Mockito.when(settlement.getBatchId()).thenReturn(1L);
        Mockito.when(settlement.isPayRequested()).thenReturn(false);
        Mockito.when(settlement.isReady()).thenReturn(true);
        Mockito.when(settlement.isHoldActive()).thenReturn(false);
        Mockito.when(settlement.getStatus()).thenReturn(SettlementStatus.READY);

        Mockito.when(settlementRepository.findById("S1")).thenReturn(Optional.of(settlement));

        Mockito.when(settlementBatchRepository.existsByBatchIdAndResult(1L, SettlementBatchResult.FAIL))
                .thenReturn(false);

        Mockito.when(refundAdjustmentPolicy.isRefundAdjustmentPending("S1")).thenReturn(true);

        assertThatThrownBy(() -> service.requestPaid("S1", "memo"))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> {
                    ConflictException ce = (ConflictException) ex;
                    assertThat(ce.toErrorResponse().reason()).isEqualTo(ReasonCode.REFUND_ADJUSTMENT_PENDING.name());
                });

        Mockito.verifyNoInteractions(auditLogger);

        verify(settlementBatchRepository, times(1))
                .existsByBatchIdAndResult(1L, SettlementBatchResult.FAIL);
        verify(refundAdjustmentPolicy, times(1)).isRefundAdjustmentPending("S1");
    }

    @Test
    void requestPaid_settlementNotFound_then404() {
        MDC.put("requestId", "test-request-id");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin1", "N/A")
        );

        Mockito.when(settlementRepository.findById("S404")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requestPaid("S404", null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode().value()).isEqualTo(404);
                });
    }

    @Test
    void requestPaid_requestIdMissing_thenFailFast() {
        // requestId 미세팅
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin1", "N/A")
        );

        Settlement settlement = Mockito.mock(Settlement.class);
        Mockito.when(settlement.isPayRequested()).thenReturn(true);

        // toPayActionResponse에서 필요한 최소 스텁
        Mockito.when(settlement.getSettlementId()).thenReturn("S1");
        Mockito.when(settlement.getStatus()).thenReturn(SettlementStatus.PAY_REQUESTED);
        Mockito.when(settlement.getPaidRequestedAt()).thenReturn(LocalDateTime.now());

        Mockito.when(settlementRepository.findById("S1")).thenReturn(Optional.of(settlement));

        assertThatThrownBy(() -> service.requestPaid("S1", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("requestId must not be null/blank");
    }

    @Test
    void requestPaid_settlementIdBlank_thenFailFast() {
        MDC.put("requestId", "test-request-id");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin1", "N/A")
        );

        assertThatThrownBy(() -> service.requestPaid("   ", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("settlementId must not be null/blank");
    }

    @Test
    void requestPaid_noop_payRequested_commentNull_thenAuditMetaJsonContainsCommentKey() {
        MDC.put("requestId", "test-request-id");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "N/A")
        );

        Settlement settlement = Mockito.mock(Settlement.class);
        Mockito.when(settlement.isPayRequested()).thenReturn(true);
        Mockito.when(settlement.getStatus()).thenReturn(SettlementStatus.PAY_REQUESTED);
        Mockito.when(settlement.getPaidRequestedAt()).thenReturn(LocalDateTime.now());
        Mockito.when(settlement.getSettlementId()).thenReturn("S1");
        Mockito.when(settlement.getMerchantId()).thenReturn("M1");

        Mockito.when(settlementRepository.findById("S1")).thenReturn(Optional.of(settlement));

        ArgumentCaptor<AuditLogCommand> captor = ArgumentCaptor.forClass(AuditLogCommand.class);

        service.requestPaid("S1", null);

        verify(auditLogger, times(1)).log(captor.capture());
        String metaJson = captor.getValue().getMetaJson();

        assertThat(metaJson).contains("\"comment\"");

        // no-op 경로에서는 policy 호출이 없어야 한다는 Freeze 의도를 박아두기
        Mockito.verifyNoInteractions(refundAdjustmentPolicy);
    }

    @Test
    void requestPaid_holdActive_then409_HOLD_ACTIVE() {
        MDC.put("requestId", "test-request-id");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin1", "N/A")
        );

        Settlement settlement = Mockito.mock(Settlement.class);

        Mockito.when(settlement.getSettlementId()).thenReturn("S1");
        Mockito.when(settlement.getMerchantId()).thenReturn("M1");
        Mockito.when(settlement.getBatchId()).thenReturn(1L);

        Mockito.when(settlement.isPayRequested()).thenReturn(false);

        // 핵심: HOLD가 우선
        Mockito.when(settlement.isHoldActive()).thenReturn(true);

        // READY=false여도 HOLD가 먼저 나가야 함
        Mockito.when(settlement.isReady()).thenReturn(false);

        Mockito.when(settlement.getStatus()).thenReturn(SettlementStatus.HOLD_ACTIVE);

        Mockito.when(settlementRepository.findById("S1")).thenReturn(Optional.of(settlement));

        assertThatThrownBy(() -> service.requestPaid("S1", "memo"))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> {
                    ConflictException ce = (ConflictException) ex;
                    assertThat(ce.toErrorResponse().reason()).isEqualTo(ReasonCode.HOLD_ACTIVE.name());
                });

        // HOLD에서 뒤쪽 guard(policy)까지 가면 안 됨
        Mockito.verifyNoInteractions(refundAdjustmentPolicy);

        verify(settlementBatchRepository, Mockito.never())
                .existsByBatchIdAndResult(Mockito.anyLong(), Mockito.eq(SettlementBatchResult.FAIL));

        // (정책상 실패 케이스 audit 안 남기면 유지)
        Mockito.verifyNoInteractions(auditLogger);
    }
}