package com.settleops.domain.refund.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.settleops.domain.refund.api.dto.AdminRefundDecisionResponseDTO;
import com.settleops.domain.refund.domain.Refund;
import com.settleops.domain.refund.domain.RefundStatus;
import com.settleops.domain.refund.infra.RefundEventRepository;
import com.settleops.domain.refund.infra.RefundRepository;
import com.settleops.global.audit.AuditLogCommand;
import com.settleops.global.audit.AuditLogger;
import com.settleops.global.error.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class RefundAdminServiceTest {

    private RefundRepository refundRepository;
    private RefundEventRepository refundEventRepository;
    private AuditLogger auditLogger;
    private ObjectMapper objectMapper;

    private RefundAdminService service;

    @BeforeEach
    void setUp() {
        refundRepository = mock(RefundRepository.class);
        refundEventRepository = mock(RefundEventRepository.class);
        auditLogger = mock(AuditLogger.class);
        objectMapper = new ObjectMapper();

        service = new RefundAdminService(
                refundRepository,
                refundEventRepository,
                auditLogger,
                objectMapper
        );
    }

    @Test
    void approve_noop_when_already_approved_should_return_status_decidedAt_requestId() {
        // given
        LocalDateTime decidedAt = LocalDateTime.of(2026, 3, 6, 10, 0, 0);

        Refund refund = Refund.builder()
                .refundId("refund-1")
                .paymentId("payment-1")
                .merchantId("merchant-1")
                .buyerId("buyer-1")
                .amount(1000L)
                .currency("KRW")
                .status(RefundStatus.APPROVED)
                .reasonText("buyer canceled")
                .requestedAt(LocalDateTime.of(2026, 3, 5, 10, 0, 0))
                .decidedAt(decidedAt)
                .build();

        when(refundRepository.findById("refund-1")).thenReturn(Optional.of(refund));

        // when
        AdminRefundDecisionResponseDTO response =
                service.approve("refund-1", "admin-1", "approved", "req-123");

        // then
        assertThat(response.getStatus()).isEqualTo(RefundStatus.APPROVED);
        assertThat(response.getDecidedAt()).isEqualTo(decidedAt);
        assertThat(response.getRequestId()).isEqualTo("req-123");

        verify(refundRepository).findById("refund-1");
        verify(refundEventRepository, never()).save(any());

        ArgumentCaptor<AuditLogCommand> captor = ArgumentCaptor.forClass(AuditLogCommand.class);
        verify(auditLogger).log(captor.capture());

        AuditLogCommand cmd = captor.getValue();
        assertThat(cmd.getRequestId()).isEqualTo("req-123");
        assertThat(cmd.getStatusBefore()).isEqualTo("APPROVED");
        assertThat(cmd.getStatusAfter()).isEqualTo("APPROVED");
        assertThat(cmd.getMetaJson()).contains("\"noOp\":true");
        assertThat(cmd.getMetaJson()).contains("ALREADY_APPROVED");
    }

    @Test
    void reject_noop_when_already_rejected_should_return_status_decidedAt_requestId() {
        // given
        LocalDateTime decidedAt = LocalDateTime.of(2026, 3, 6, 11, 0, 0);

        Refund refund = Refund.builder()
                .refundId("refund-2")
                .paymentId("payment-2")
                .merchantId("merchant-2")
                .buyerId("buyer-2")
                .amount(2000L)
                .currency("KRW")
                .status(RefundStatus.REJECTED)
                .reasonText("duplicate")
                .requestedAt(LocalDateTime.of(2026, 3, 5, 9, 0, 0))
                .decidedAt(decidedAt)
                .build();

        when(refundRepository.findById("refund-2")).thenReturn(Optional.of(refund));

        // when
        AdminRefundDecisionResponseDTO response =
                service.reject("refund-2", "admin-1", "rejected", "req-456");

        // then
        assertThat(response.getStatus()).isEqualTo(RefundStatus.REJECTED);
        assertThat(response.getDecidedAt()).isEqualTo(decidedAt);
        assertThat(response.getRequestId()).isEqualTo("req-456");

        verify(refundRepository).findById("refund-2");
        verify(refundEventRepository, never()).save(any());

        ArgumentCaptor<AuditLogCommand> captor = ArgumentCaptor.forClass(AuditLogCommand.class);
        verify(auditLogger).log(captor.capture());

        AuditLogCommand cmd = captor.getValue();
        assertThat(cmd.getRequestId()).isEqualTo("req-456");
        assertThat(cmd.getStatusBefore()).isEqualTo("REJECTED");
        assertThat(cmd.getStatusAfter()).isEqualTo("REJECTED");
        assertThat(cmd.getMetaJson()).contains("\"noOp\":true");
        assertThat(cmd.getMetaJson()).contains("ALREADY_REJECTED");
    }

    @Test
    void approve_requested_should_transition_to_approved_and_return_min_fields() {
        // given
        Refund refund = Refund.builder()
                .refundId("refund-3")
                .paymentId("payment-3")
                .merchantId("merchant-3")
                .buyerId("buyer-3")
                .amount(3000L)
                .currency("KRW")
                .status(RefundStatus.REQUESTED)
                .reasonText("product issue")
                .requestedAt(LocalDateTime.of(2026, 3, 5, 12, 0, 0))
                .decidedAt(null)
                .build();

        when(refundRepository.findById("refund-3")).thenReturn(Optional.of(refund));

        // when
        AdminRefundDecisionResponseDTO response =
                service.approve("refund-3", "admin-1", "approve ok", "req-789");

        // then
        assertThat(response.getStatus()).isEqualTo(RefundStatus.APPROVED);
        assertThat(response.getDecidedAt()).isNotNull();
        assertThat(response.getRequestId()).isEqualTo("req-789");

        assertThat(refund.getStatus()).isEqualTo(RefundStatus.APPROVED);
        assertThat(refund.getDecidedAt()).isNotNull();

        verify(refundEventRepository).save(any());
        verify(auditLogger).log(any(AuditLogCommand.class));
    }

    @Test
    void approve_blank_comment_should_throw_bad_request() {
        assertThatThrownBy(() ->
                service.approve("refund-1", "admin-1", "   ", "req-123"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("comment is required");

        verifyNoInteractions(refundRepository, refundEventRepository, auditLogger);
    }

    @Test
    void approve_blank_requestId_should_throw_bad_request() {
        assertThatThrownBy(() ->
                service.approve("refund-1", "admin-1", "ok", "   "))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("requestId is null/blank");

        verifyNoInteractions(refundRepository, refundEventRepository, auditLogger);
    }
}