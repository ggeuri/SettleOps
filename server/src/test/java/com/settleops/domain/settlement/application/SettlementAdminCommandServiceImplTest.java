package com.settleops.domain.settlement.application;

import com.settleops.domain.settlement.entity.Settlement;
import com.settleops.domain.settlement.enums.SettlementStatus;
import com.settleops.domain.settlement.infra.SettlementBatchRepository;
import com.settleops.domain.settlement.infra.SettlementRepository;
import com.settleops.global.audit.AuditLogger;
import com.settleops.global.enums.ReasonCode;
import com.settleops.global.error.BadRequestException;
import com.settleops.global.error.ConflictException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

class SettlementAdminCommandServiceImplTest {

    private SettlementRepository settlementRepository;
    private SettlementBatchRepository settlementBatchRepository;
    private AuditLogger auditLogger;
    private RefundAdjustmentPolicy refundAdjustmentPolicy;

    private SettlementAdminCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        settlementRepository = Mockito.mock(SettlementRepository.class);
        settlementBatchRepository = Mockito.mock(SettlementBatchRepository.class);
        auditLogger = Mockito.mock(AuditLogger.class);
        refundAdjustmentPolicy = Mockito.mock(RefundAdjustmentPolicy.class);

        // PR#2 기준: ObjectMapper 없는 생성자 사용
        service = new SettlementAdminCommandServiceImpl(
                settlementRepository,
                settlementBatchRepository,
                auditLogger,
                refundAdjustmentPolicy
        );

        MDC.put("requestId", "test-request-id");
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
    void requestPaid_holdActive_should_return_409_HOLD_ACTIVE_even_if_not_ready() {
        Settlement settlement = Mockito.mock(Settlement.class);

        // 공통 스텁(서비스가 접근할 수 있는 필드들)
        Mockito.when(settlement.getSettlementId()).thenReturn("S1");
        Mockito.when(settlement.getMerchantId()).thenReturn("M1");
        Mockito.when(settlement.getBatchId()).thenReturn(1L);
        Mockito.when(settlement.getStatus()).thenReturn(SettlementStatus.HOLD_ACTIVE);

        Mockito.when(settlement.isPayRequested()).thenReturn(false);

        // 핵심: HOLD가 우선(READY=false여도 HOLD 먼저)
        Mockito.when(settlement.isHoldActive()).thenReturn(true);
        Mockito.when(settlement.isReady()).thenReturn(false);

        Mockito.when(settlementRepository.findById("S1")).thenReturn(Optional.of(settlement));

        assertThatThrownBy(() -> service.requestPaid("S1", "memo"))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> {
                    ConflictException ce = (ConflictException) ex;
                    assertThat(ce.toErrorResponse().reason()).isEqualTo(ReasonCode.HOLD_ACTIVE.name());
                });

        // HOLD에서 뒤쪽 가드로 진행하면 안 됨
        verifyNoInteractions(refundAdjustmentPolicy);
        verifyNoInteractions(settlementBatchRepository);
        verifyNoInteractions(auditLogger);
    }

    @Test
    void requestPaid_notReady_should_return_409_SETTLEMENT_NOT_READY() {
        Settlement settlement = Mockito.mock(Settlement.class);

        Mockito.when(settlement.getSettlementId()).thenReturn("S1");
        Mockito.when(settlement.getMerchantId()).thenReturn("M1");
        Mockito.when(settlement.getBatchId()).thenReturn(1L);
        Mockito.when(settlement.getStatus()).thenReturn(SettlementStatus.READY);

        Mockito.when(settlement.isPayRequested()).thenReturn(false);

        // 핵심: HOLD=false, READY=false → NOT_READY
        Mockito.when(settlement.isHoldActive()).thenReturn(false);
        Mockito.when(settlement.isReady()).thenReturn(false);

        Mockito.when(settlementRepository.findById("S1")).thenReturn(Optional.of(settlement));

        assertThatThrownBy(() -> service.requestPaid("S1", "memo"))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> {
                    ConflictException ce = (ConflictException) ex;
                    assertThat(ce.toErrorResponse().reason()).isEqualTo(ReasonCode.SETTLEMENT_NOT_READY.name());
                });

        // NOT_READY에서 뒤쪽 가드로 진행하면 안 됨
        verifyNoInteractions(refundAdjustmentPolicy);
        verifyNoInteractions(settlementBatchRepository);
        verifyNoInteractions(auditLogger);
    }

    // 옵션(권장): 400 계약을 “예외 타입”으로 고정해두면 PR 코멘트 대응이 더 깔끔해짐
    @Test
    void requestPaid_settlementId_blank_should_throw_BadRequestException() {
        assertThatThrownBy(() -> service.requestPaid("   ", "memo"))
                .isInstanceOf(BadRequestException.class);
    }
}