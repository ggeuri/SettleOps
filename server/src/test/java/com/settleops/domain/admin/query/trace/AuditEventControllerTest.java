package com.settleops.domain.admin.query.trace;

import com.settleops.domain.admin.query.trace.controller.AuditEventController;
import com.settleops.domain.admin.query.trace.dto.AuditEventResponseDto;
import com.settleops.domain.admin.query.trace.dto.AuditEventRowDto;
import com.settleops.domain.admin.query.trace.service.AuditEventQueryService;
import com.settleops.global.audit.AuditLogger;
import com.settleops.global.audit.EntityType;
import com.settleops.global.auth.SessionAuthProvider;
import com.settleops.global.error.BadRequestException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuditEventController.class)
class AuditEventControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AuditEventQueryService auditEventQueryService;

    @MockitoBean
    SessionAuthProvider sessionAuthProvider;

    @MockitoBean
    AuditLogger auditLogger;

    @Test
    @WithMockUser(roles = "ADMIN")
    void requestId_should_return_200_with_items() throws Exception {
        // Given
        AuditEventRowDto row1 = AuditEventRowDto.of(
                LocalDateTime.parse("2026-03-22T10:00:00"),
                "PAYMENT_CONFIRMED",
                EntityType.PAYMENT,
                "payment-1",
                "CAPTURED",
                "CAPTURED",
                "{}"
        );

        AuditEventRowDto row2 = AuditEventRowDto.of(
                LocalDateTime.parse("2026-03-22T10:01:00"),
                "HOLD_APPROVED",
                EntityType.HOLD,
                "hold-1",
                "HOLD_REQUESTED",
                "HOLD_ACTIVE",
                "{\"comment\":\"승인\"}"
        );

        AuditEventResponseDto response = new AuditEventResponseDto(
                "req-1",
                List.of(row1, row2)
        );

        given(auditEventQueryService.findByRequestId("req-1"))
                .willReturn(response);

        // When / Then
        mockMvc.perform(get("/api/admin/audit-events")
                        .param("requestId", "req-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("req-1"))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].eventType").value("PAYMENT_CONFIRMED"))
                .andExpect(jsonPath("$.items[0].entityType").value("PAYMENT"))
                .andExpect(jsonPath("$.items[0].entityId").value("payment-1"))
                .andExpect(jsonPath("$.items[0].statusBefore").value("CAPTURED"))
                .andExpect(jsonPath("$.items[0].statusAfter").value("CAPTURED"))
                .andExpect(jsonPath("$.items[0].metaJson").value("{}"))
                .andExpect(jsonPath("$.items[1].eventType").value("HOLD_APPROVED"))
                .andExpect(jsonPath("$.items[1].entityType").value("HOLD"))
                .andExpect(jsonPath("$.items[1].entityId").value("hold-1"));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(auditEventQueryService, times(1)).findByRequestId(captor.capture());
        assertEquals("req-1", captor.getValue());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void blank_requestId_should_return_400() throws Exception {
        // Given
        given(auditEventQueryService.findByRequestId(anyString()))
                .willThrow(new BadRequestException("requestId는 필수입니다."));

        // When / Then
        mockMvc.perform(get("/api/admin/audit-events")
                        .param("requestId", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.reason").value(nullValue()))
                .andExpect(jsonPath("$.message").value("requestId는 필수입니다."));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void missing_requestId_should_return_400_by_spring_binding() throws Exception {
        mockMvc.perform(get("/api/admin/audit-events"))
                .andExpect(status().isBadRequest());
    }
}