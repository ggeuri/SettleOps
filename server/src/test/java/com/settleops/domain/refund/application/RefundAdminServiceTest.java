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
import java.util.Map;
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
    void approve_noop_when_already_approved_should_return_status_decidedAt_requestId(){
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

        AdminRefundDecisionResponseDTO response =
                service.approve("refund-1", "admin-1", "approved", "req-123");

        // LOCKED: no-op 200 응답은 status + decidedAt + requestId를 유지해야 한다.
        assertThat(response.getStatus()).isEqualTo(RefundStatus.APPROVED);
        assertThat(response.getDecidedAt()).isEqualTo(decidedAt);
        assertThat(response.getRequestId()).isEqualTo("req-123");

        verify(refundRepository).findById("refund-1");
        verify(refundEventRepository, never()).save(any());
        verify(refundRepository, never()).save(any());

        ArgumentCaptor<AuditLogCommand> captor = ArgumentCaptor.forClass(AuditLogCommand.class);
        verify(auditLogger).log(captor.capture());

        AuditLogCommand cmd = captor.getValue();

        // LOCKED: no-op도 audit_log에 반드시 기록되어야 한다.
        assertThat(cmd.getRequestId()).isEqualTo("req-123");
        assertThat(cmd.getStatusBefore()).isEqualTo("APPROVED");
        assertThat(cmd.getStatusAfter()).isEqualTo("APPROVED");
        assertThat(cmd.getMetaJson()).contains("\"noOp\":true");
        assertThat(cmd.getMetaJson()).contains("ALREADY_APPROVED");

    }

    @Test
    void reject_noop_when_already_rejected_should_return_status_decidedAt_requestId(){
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

        AdminRefundDecisionResponseDTO response =
                service.reject("refund-2", "admin-1", "rejected", "req-456");

        // LOCKED: no-op 200 응답은 status + decidedAt + requestId를 유지해야 한다.
        assertThat(response.getStatus()).isEqualTo(RefundStatus.REJECTED);
        assertThat(response.getDecidedAt()).isEqualTo(decidedAt);
        assertThat(response.getRequestId()).isEqualTo("req-456");

        verify(refundRepository).findById("refund-2");
        verify(refundRepository, never()).save(any());
        verifyNoInteractions(refundEventRepository);

        ArgumentCaptor<AuditLogCommand> captor = ArgumentCaptor.forClass(AuditLogCommand.class);
        verify(auditLogger).log(captor.capture());

        AuditLogCommand cmd = captor.getValue();

        // LOCKED: no-op도 audit_log에 반드시 기록되어야 한다.
        assertThat(cmd.getRequestId()).isEqualTo("req-456");
        assertThat(cmd.getStatusBefore()).isEqualTo("REJECTED");
        assertThat(cmd.getStatusAfter()).isEqualTo("REJECTED");
        assertThat(cmd.getMetaJson()).contains("\"noOp\":true");
        assertThat(cmd.getMetaJson()).contains("ALREADY_REJECTED");
    }

    @Test
    void approve_requested_should_transition_to_approved_and_return_min_fields(){
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

        String comment = "approve ok";

        AdminRefundDecisionResponseDTO response =
                service.approve("refund-3", "admin-1", comment, "req-789");

        assertThat(response.getStatus()).isEqualTo(RefundStatus.APPROVED);
        assertThat(response.getDecidedAt()).isNotNull();
        assertThat(response.getRequestId()).isEqualTo("req-789");

        // LOCKED: approve 응답 최소필드 검증
        assertThat(refund.getStatus()).isEqualTo(RefundStatus.APPROVED);
        assertThat(refund.getDecidedAt()).isNotNull();

        ArgumentCaptor<com.settleops.domain.refund.domain.RefundEvent> eventCaptor =
                ArgumentCaptor.forClass(com.settleops.domain.refund.domain.RefundEvent.class);
        verify(refundEventRepository, times(1)).save(eventCaptor.capture());
        verifyNoMoreInteractions(refundEventRepository);

        com.settleops.domain.refund.domain.RefundEvent savedEvent = eventCaptor.getValue();

        // LOCKED: REFUND_APPROVED 이벤트는 request_id / actor / status 전이를 정확히 남겨야 한다.
        assertThat(savedEvent.getEventType())
                .isEqualTo(com.settleops.domain.refund.domain.RefundEventType.REFUND_APPROVED);
        assertThat(savedEvent.getStatusBefore())
                .isEqualTo(RefundStatus.REQUESTED);
        assertThat(savedEvent.getStatusAfter())
                .isEqualTo(RefundStatus.APPROVED);
        assertThat(savedEvent.getRequestId())
                .isEqualTo("req-789");
        assertThat(savedEvent.getActorType())
                .isEqualTo(com.settleops.global.audit.ActorType.ADMIN);
        assertThat(savedEvent.getActorId())
                .isEqualTo("admin-1");

        ArgumentCaptor<AuditLogCommand> auditCaptor =
                ArgumentCaptor.forClass(AuditLogCommand.class);
        verify(auditLogger).log(auditCaptor.capture());

        AuditLogCommand cmd = auditCaptor.getValue();

        // LOCKED: audit_log는 request_id 기준으로 상태 전이를 재현할 수 있어야 한다.
        assertThat(cmd.getRequestId()).isEqualTo("req-789");
        assertThat(cmd.getStatusBefore()).isEqualTo("REQUESTED");
        assertThat(cmd.getStatusAfter()).isEqualTo("APPROVED");
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
    @Test
    void reject_requested_should_transition_to_rejected_and_save_event(){

        Refund refund = Refund.builder()
                .refundId("refund-6")
                .paymentId("payment-6")
                .merchantId("merchant-6")
                .buyerId("buyer-6")
                .amount(5000L)
                .currency("KRW")
                .status(RefundStatus.REQUESTED) // 아직 결정 전
                .reasonText("duplicate")
                .requestedAt(LocalDateTime.of(2026, 3, 5, 12, 0, 0))
                .decidedAt(null)
                .build();

        when(refundRepository.findById("refund-6")).thenReturn(Optional.of(refund));

        String comment = "reject ok";

        AdminRefundDecisionResponseDTO response =
                service.reject("refund-6", "admin-1", comment, "req-999");

        // LOCKED: reject 응답 최소필드 검증
        assertThat(response.getStatus()).isEqualTo(RefundStatus.REJECTED);
        assertThat(response.getDecidedAt()).isNotNull();
        assertThat(response.getRequestId()).isEqualTo("req-999");

        ArgumentCaptor<com.settleops.domain.refund.domain.RefundEvent> eventCaptor =
                ArgumentCaptor.forClass(com.settleops.domain.refund.domain.RefundEvent.class);
        verify(refundEventRepository, times(1)).save(eventCaptor.capture());
        verifyNoMoreInteractions(refundEventRepository);

        com.settleops.domain.refund.domain.RefundEvent savedEvent = eventCaptor.getValue();

        // LOCKED: REFUND_REJECTED 이벤트는 request_id / actor / status 전이를 정확히 남겨야 한다.
        assertThat(savedEvent.getEventType())
                .isEqualTo(com.settleops.domain.refund.domain.RefundEventType.REFUND_REJECTED);
        assertThat(savedEvent.getStatusBefore()).isEqualTo(RefundStatus.REQUESTED);
        assertThat(savedEvent.getStatusAfter()).isEqualTo(RefundStatus.REJECTED);
        assertThat(savedEvent.getRequestId()).isEqualTo("req-999");
        assertThat(savedEvent.getActorId()).isEqualTo("admin-1");

        ArgumentCaptor<AuditLogCommand> auditCaptor =
                ArgumentCaptor.forClass(AuditLogCommand.class);
        verify(auditLogger).log(auditCaptor.capture());

        AuditLogCommand cmd = auditCaptor.getValue();

        // LOCKED: audit_log는 request_id 기준으로 상태 전이를 재현할 수 있어야 한다.
        assertThat(cmd.getRequestId()).isEqualTo("req-999");
        assertThat(cmd.getStatusBefore()).isEqualTo("REQUESTED");
        assertThat(cmd.getStatusAfter()).isEqualTo("REJECTED");
    }
    @Test
    void approve_should_write_comment_and_before_after_diff_into_audit_meta_json() throws Exception {

        //승인 시 운영 재현용 audit meta가 문서대로 남는지 테스트

        String refundId = "refund-1";
        String adminId = "admin01";
        String requestId = "req-123";
        String comment = "증빙 확인 후 승인";

        Refund refund = Refund.builder()
                .refundId("refund-1")
                .merchantId("merchant-1")
                .status(RefundStatus.REQUESTED)
                .requestedAt(LocalDateTime.now().minusMinutes(5))
                .build();

        when(refundRepository.findById(refundId)).thenReturn(Optional.of(refund));

        AdminRefundDecisionResponseDTO response =
                service.approve(refundId, adminId, comment, requestId);

        assertThat(response.getRequestId()).isEqualTo(requestId);
        assertThat(response.getStatus()).isEqualTo(RefundStatus.APPROVED);

        ArgumentCaptor<AuditLogCommand> captor = ArgumentCaptor.forClass(AuditLogCommand.class);
        verify(auditLogger).log(captor.capture());

        AuditLogCommand command = captor.getValue();

        Map<String, Object> meta = objectMapper.readValue(command.getMetaJson(), Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> before = (Map<String, Object>) meta.get("before");

        @SuppressWarnings("unchecked")
        Map<String, Object> after = (Map<String, Object>) meta.get("after");

        assertThat(meta.get("comment")).isEqualTo(comment);
        assertThat(meta.get("noOp")).isEqualTo(false);
        assertThat(before.get("status")).isEqualTo("REQUESTED");
        assertThat(after.get("status")).isEqualTo("APPROVED");
    }
}