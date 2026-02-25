package com.settleops.domain.settlement.application;

import com.settleops.domain.settlement.entity.Settlement;
import com.settleops.domain.settlement.entity.SettlementBatch;
import com.settleops.domain.settlement.enums.SettlementBatchResult;
import com.settleops.domain.settlement.enums.SettlementStatus;
import com.settleops.domain.settlement.infra.SettlementBatchRepository;
import com.settleops.domain.settlement.infra.SettlementRepository;
import com.settleops.global.audit.AuditLogger;
import com.settleops.global.enums.ReasonCode;
import com.settleops.global.error.ConflictException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class SettlementAdminCommandServiceImplTest {
    private final SettlementRepository settlementRepository = Mockito.mock(SettlementRepository.class);
    private final SettlementBatchRepository settlementBatchRepository = Mockito.mock(SettlementBatchRepository.class);
    private final AuditLogger auditLogger = Mockito.mock(AuditLogger.class);
    private final RefundAdjustmentPolicy refundAdjustmentPolicy = Mockito.mock(RefundAdjustmentPolicy.class);

    private final SettlementAdminCommandServiceImpl service = new SettlementAdminCommandServiceImpl(settlementRepository, settlementBatchRepository, auditLogger, refundAdjustmentPolicy);

    @AfterEach
    void tearDown(){
        MDC.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void requestPaid_refundAdjustmentPending_true_then409_REFUND_ADJUSTMENT_PENDING(){
        // requestId 필수(LOCKED)
        MDC.put("requestId", "test-request-id");
        // actorId(SecurityContext)
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

        SettlementBatch batch = Mockito.mock(SettlementBatch.class);
        Mockito.when(batch.getResult()).thenReturn(SettlementBatchResult.OK);
        Mockito.when(settlementBatchRepository.findById(1L)).thenReturn(Optional.of(batch));

        Mockito.when(refundAdjustmentPolicy.isRefundAdjustmentPending("S1")).thenReturn(true);

        assertThatThrownBy(() -> service.requestPaid("S1", "memo"))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> {
                    ConflictException ce = (ConflictException) ex;
                    assertThat(ce.toErrorResponse().reason()).isEqualTo(ReasonCode.REFUND_ADJUSTMENT_PENDING.name());
                });
        Mockito.verify(refundAdjustmentPolicy, Mockito.times(1))
                .isRefundAdjustmentPending("S1");

        Mockito.verifyNoMoreInteractions(refundAdjustmentPolicy);
    }

    @Test
    void requestPaid_settlementNotFound_then404(){
        MDC.put("requestId", "test-request-id");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin1", "N/A")
        );

        Mockito.when(settlementRepository.findById("S404")).thenReturn(Optional.empty());

        assertThatThrownBy(()-> service.requestPaid("S404", null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode().value()).isEqualTo(404);
                });
    }

    @Test
    void requestPaid_requestIdMissing_thenFailFast(){
        //actorId만 세팅
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin1", "N/A")
        );

        Settlement settlement = Mockito.mock(Settlement.class);
        Mockito.when(settlement.isPayRequested()).thenReturn(true); // no-op 경로에서도 toPayActionResponse 호출로 requestId 읽음
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
}
