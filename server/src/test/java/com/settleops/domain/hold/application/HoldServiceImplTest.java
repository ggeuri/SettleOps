package com.settleops.domain.hold.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.settleops.domain.hold.domain.Hold;
import com.settleops.domain.hold.domain.HoldReasonCode;
import com.settleops.domain.hold.domain.HoldStatus;
import com.settleops.domain.hold.infra.HoldEventRepository;
import com.settleops.domain.hold.infra.HoldRepository;
import com.settleops.domain.settlement.entity.Settlement;
import com.settleops.domain.settlement.infra.SettlementRepository;
import com.settleops.global.audit.AuditLogger;
import com.settleops.global.error.ConflictException;
import com.settleops.global.error.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * HoldServiceImpl 단위 테스트
 *
 * 현재 기준선:
 * - approve: HOLD_REQUESTED -> HOLD_ACTIVE
 * - release: HOLD_ACTIVE -> RELEASED + settlement READY 복구
 * - no-op 200: approve(HOLD_ACTIVE/RELEASED), release(RELEASED)
 * - 가드 추가:
 *   1) approve 시 settlement는 READY여야 함
 *   2) release 시 settlement는 HOLD_ACTIVE여야 함
 */
@ExtendWith(MockitoExtension.class)
class HoldServiceImplTest {

    @Mock
    private HoldRepository holdRepository;

    @Mock
    private HoldEventRepository holdEventRepository;

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private AuditLogger auditLogger;

    private HoldServiceImpl holdService;

    @BeforeEach
    void setUp() {
        holdService = new HoldServiceImpl(
                holdRepository,
                holdEventRepository,
                settlementRepository,
                auditLogger,
                new ObjectMapper()
        );
    }

    @Test
    @DisplayName("createHold - settlement가 PAID면 ConflictException")
    void createHold_whenSettlementIsPaid_shouldThrowConflict() {
        // given
        Settlement paidSettlement = paidSettlement("settlement-1", "merchant-1");

        HoldCreateCommand command = new HoldCreateCommand(
                "req-1",
                "admin1",
                "settlement-1",
                HoldReasonCode.MANUAL_REVIEW,
                "hold 요청"
        );

        when(settlementRepository.findById("settlement-1"))
                .thenReturn(Optional.of(paidSettlement));

        // when
        ConflictException ex = assertThrows(
                ConflictException.class,
                () -> holdService.createHold(command)
        );

        // then
        assertTrue(ex.getMessage().contains("PAID"));
        verify(holdRepository, never()).save(any());
        verify(holdEventRepository, never()).save(any());
        verify(auditLogger, never()).log(any());
    }

    @Test
    @DisplayName("createHold - 같은 settlement에 hold가 이미 있으면 ConflictException")
    void createHold_whenHoldAlreadyExists_shouldThrowConflict() {
        // given
        Settlement readySettlement = readySettlement("settlement-1", "merchant-1");

        HoldCreateCommand command = new HoldCreateCommand(
                "req-1",
                "admin1",
                "settlement-1",
                HoldReasonCode.MANUAL_REVIEW,
                "hold 요청"
        );

        when(settlementRepository.findById("settlement-1"))
                .thenReturn(Optional.of(readySettlement));
        when(holdRepository.existsBySettlementId("settlement-1"))
                .thenReturn(true);

        // when
        ConflictException ex = assertThrows(
                ConflictException.class,
                () -> holdService.createHold(command)
        );

        // then
        assertTrue(ex.getMessage().contains("이미 hold가 존재"));
        verify(holdRepository, never()).save(any());
        verify(holdEventRepository, never()).save(any());
        verify(auditLogger, never()).log(any());
    }

    @Test
    @DisplayName("approveHold - HOLD_REQUESTED여도 settlement가 READY가 아니면 ConflictException")
    void approveHold_whenSettlementIsNotReady_shouldThrowConflict() {
        // given
        Hold hold = requestedHold("settlement-1");
        Settlement payRequestedSettlement = payRequestedSettlement("settlement-1", "merchant-1");

        HoldApproveCommand command = new HoldApproveCommand(
                "req-approve-1",
                "admin1",
                "승인"
        );

        when(holdRepository.findById(hold.getHoldId()))
                .thenReturn(Optional.of(hold));
        when(settlementRepository.findById("settlement-1"))
                .thenReturn(Optional.of(payRequestedSettlement));

        // when
        ConflictException ex = assertThrows(
                ConflictException.class,
                () -> holdService.approveHold(hold.getHoldId(), command)
        );

        // then
        assertTrue(ex.getMessage().contains("READY 상태"));
        assertEquals(HoldStatus.HOLD_REQUESTED, hold.getStatus());
        assertFalse(payRequestedSettlement.isHoldActive());

        verify(holdEventRepository, never()).save(any());
        verify(auditLogger, never()).log(any());
    }

    @Test
    @DisplayName("releaseHold - hold는 HOLD_ACTIVE여도 settlement가 HOLD_ACTIVE가 아니면 ConflictException")
    void releaseHold_whenSettlementIsNotHoldActive_shouldThrowConflict() {
        // given
        Hold hold = activeHold("settlement-1");
        Settlement readySettlement = readySettlement("settlement-1", "merchant-1");

        HoldReleaseCommand command = new HoldReleaseCommand(
                "req-release-1",
                "admin1",
                "해제"
        );

        when(holdRepository.findById(hold.getHoldId()))
                .thenReturn(Optional.of(hold));
        when(settlementRepository.findById("settlement-1"))
                .thenReturn(Optional.of(readySettlement));

        // when
        ConflictException ex = assertThrows(
                ConflictException.class,
                () -> holdService.releaseHold(hold.getHoldId(), command)
        );

        // then
        assertTrue(ex.getMessage().contains("HOLD_ACTIVE"));
        assertEquals(HoldStatus.HOLD_ACTIVE, hold.getStatus());
        assertTrue(readySettlement.isReady());

        verify(holdEventRepository, never()).save(any());
        verify(auditLogger, never()).log(any());
    }

    @Test
    @DisplayName("approveHold - hold가 이미 HOLD_ACTIVE면 no-op 응답 반환")
    void approveHold_whenAlreadyActive_shouldReturnNoOpResponse() {
        // given
        Hold hold = activeHold("settlement-1");
        Settlement holdActiveSettlement = holdActiveSettlement("settlement-1", "merchant-1");

        HoldApproveCommand command = new HoldApproveCommand(
                "req-approve-2",
                "admin1",
                "재승인"
        );

        when(holdRepository.findById(hold.getHoldId()))
                .thenReturn(Optional.of(hold));
        when(settlementRepository.findById("settlement-1"))
                .thenReturn(Optional.of(holdActiveSettlement));

        // when
        HoldDecisionResponse response = holdService.approveHold(hold.getHoldId(), command);

        // then
        assertEquals("req-approve-2", response.requestId());
        assertEquals(HoldStatus.HOLD_ACTIVE, response.status());
        assertNotNull(response.decidedAt());

        verify(holdEventRepository, never()).save(any());
        verify(auditLogger, times(1)).log(any());
    }

    @Test
    @DisplayName("releaseHold - HOLD_ACTIVE면 RELEASED 전이 + settlement READY 복구")
    void releaseHold_whenActive_shouldReleaseAndRestoreSettlementReady() {
        // given
        Hold hold = activeHold("settlement-1");
        Settlement settlement = holdActiveSettlement("settlement-1", "merchant-1");

        HoldReleaseCommand command = new HoldReleaseCommand(
                "req-release-2",
                "admin1",
                "정상 해제"
        );

        when(holdRepository.findById(hold.getHoldId()))
                .thenReturn(Optional.of(hold));
        when(settlementRepository.findById("settlement-1"))
                .thenReturn(Optional.of(settlement));

        // when
        HoldDecisionResponse response = holdService.releaseHold(hold.getHoldId(), command);

        // then
        assertEquals("req-release-2", response.requestId());
        assertEquals(HoldStatus.RELEASED, response.status());
        assertNotNull(response.decidedAt());

        assertEquals(HoldStatus.RELEASED, hold.getStatus());
        assertTrue(settlement.isReady());

        verify(holdEventRepository, times(1)).save(any());
        verify(auditLogger, times(1)).log(any());
    }

    @Test
    @DisplayName("releaseHold - hold가 이미 RELEASED면 no-op 응답 반환")
    void releaseHold_whenAlreadyReleased_shouldReturnNoOpResponse() {
        // given
        Hold hold = releasedHold("settlement-1");
        Settlement readySettlement = readySettlement("settlement-1", "merchant-1");

        HoldReleaseCommand command = new HoldReleaseCommand(
                "req-release-3",
                "admin1",
                "재해제"
        );

        when(holdRepository.findById(hold.getHoldId()))
                .thenReturn(Optional.of(hold));
        when(settlementRepository.findById("settlement-1"))
                .thenReturn(Optional.of(readySettlement));

        // when
        HoldDecisionResponse response = holdService.releaseHold(hold.getHoldId(), command);

        // then
        assertEquals("req-release-3", response.requestId());
        assertEquals(HoldStatus.RELEASED, response.status());
        assertNotNull(response.decidedAt());

        verify(holdEventRepository, never()).save(any());
        verify(auditLogger, times(1)).log(any());
    }

    @Test
    @DisplayName("approveHold - hold가 없으면 NotFoundException")
    void approveHold_whenHoldNotFound_shouldThrowNotFound() {
        // given
        when(holdRepository.findById("missing-hold"))
                .thenReturn(Optional.empty());

        HoldApproveCommand command = new HoldApproveCommand(
                "req-missing",
                "admin1",
                "승인"
        );

        // when / then
        assertThrows(NotFoundException.class, () -> holdService.approveHold("missing-hold", command));

        verify(settlementRepository, never()).findById(any());
        verify(holdEventRepository, never()).save(any());
        verify(auditLogger, never()).log(any());
    }

    private Settlement readySettlement(String settlementId, String merchantId) {
        return Settlement.createReady(
                settlementId,
                "SET-20260323-001",
                1L,
                merchantId,
                LocalDate.of(2026, 3, 23),
                100000L,
                3000L,
                300L,
                96700L
        );
    }

    private Settlement payRequestedSettlement(String settlementId, String merchantId) {
        Settlement settlement = readySettlement(settlementId, merchantId);
        settlement.requestPaid("requester1", LocalDateTime.of(2026, 3, 23, 10, 0, 0));
        return settlement;
    }

    private Settlement holdActiveSettlement(String settlementId, String merchantId) {
        Settlement settlement = readySettlement(settlementId, merchantId);
        settlement.markHoldActive();
        return settlement;
    }

    private Settlement paidSettlement(String settlementId, String merchantId) {
        Settlement settlement = readySettlement(settlementId, merchantId);
        settlement.requestPaid("requester1", LocalDateTime.of(2026, 3, 23, 10, 0, 0));
        settlement.approvePaid("approver2", LocalDateTime.of(2026, 3, 23, 10, 5, 0));
        return settlement;
    }

    private Hold requestedHold(String settlementId) {
        return Hold.requested(
                settlementId,
                HoldReasonCode.MANUAL_REVIEW,
                "최초 요청",
                "admin1"
        );
    }

    private Hold activeHold(String settlementId) {
        Hold hold = requestedHold(settlementId);
        hold.approve();
        return hold;
    }

    private Hold releasedHold(String settlementId) {
        Hold hold = requestedHold(settlementId);
        hold.approve();
        hold.release();
        return hold;
    }
}