package com.settleops.domain.hold;

import com.settleops.domain.hold.api.AdminHoldController;
import com.settleops.domain.hold.api.dto.AdminHoldQueueResponse;
import com.settleops.domain.hold.api.dto.AdminHoldQueueRowDto;
import com.settleops.domain.hold.api.dto.HoldQueueSearchRequestDto;
import com.settleops.domain.hold.application.HoldApproveCommand;
import com.settleops.domain.hold.application.HoldCreateCommand;
import com.settleops.domain.hold.application.HoldCreateResponse;
import com.settleops.domain.hold.application.HoldDecisionResponse;
import com.settleops.domain.hold.application.HoldQueryService;
import com.settleops.domain.hold.application.HoldReleaseCommand;
import com.settleops.domain.hold.application.HoldService;
import com.settleops.domain.hold.domain.HoldReasonCode;
import com.settleops.domain.hold.domain.HoldStatus;
import com.settleops.global.audit.AuditLogger;
import com.settleops.global.auth.SessionAuthProvider;
import com.settleops.global.auth.controller.MeController;
import com.settleops.global.auth.resolver.LoginAdminArgumentResolver;
import com.settleops.global.enums.ReasonCode;
import com.settleops.global.error.ConflictException;
import com.settleops.global.error.GlobalExceptionHandler;
import com.settleops.global.web.RequestIdResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminHoldControllerTest {

    private MockMvc mockMvc;

    @Mock
    private HoldService holdService;

    @Mock
    private HoldQueryService holdQueryService;

    @Mock
    private SessionAuthProvider sessionAuthProvider;

    @Mock
    private AuditLogger auditLogger;

    @Mock
    private RequestIdResolver requestIdResolver;

    @BeforeEach
    void setUp() {
        AdminHoldController controller =
                new AdminHoldController(holdService, holdQueryService, requestIdResolver);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(
                        new LoginAdminArgumentResolver(sessionAuthProvider),
                        new PageableHandlerMethodArgumentResolver()
                )
                .setControllerAdvice(new GlobalExceptionHandler(auditLogger))
                .build();
    }

    private MockHttpSession adminSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(MeController.SessionKeys.ROLE, "ADMIN");
        session.setAttribute(MeController.SessionKeys.ADMIN_ID, "admin1");
        return session;
    }

    @Test
    @DisplayName("POST /api/admin/holds - 정상 200 + 최소 필드 반환")
    void create_hold_should_return_200_with_minimum_fields() throws Exception {
        given(sessionAuthProvider.getCurrentAdminId()).willReturn("admin1");
        given(requestIdResolver.resolve(any())).willReturn("req-1");

        HoldCreateResponse response = new HoldCreateResponse(
                "req-1",
                "hold-1",
                "settle-1",
                HoldStatus.HOLD_REQUESTED,
                LocalDateTime.of(2026, 3, 20, 14, 0, 0)
        );

        given(holdService.createHold(any(HoldCreateCommand.class))).willReturn(response);

        String body = """
                {
                  "settlementId": "settle-1",
                  "reasonCode": "MANUAL_REVIEW",
                  "comment": "hold 요청"
                }
                """;

        mockMvc.perform(post("/api/admin/holds")
                        .session(adminSession())
                        .header("X-Request-Id", "req-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("req-1"))
                .andExpect(jsonPath("$.holdId").value("hold-1"))
                .andExpect(jsonPath("$.settlementId").value("settle-1"))
                .andExpect(jsonPath("$.status").value("HOLD_REQUESTED"))
                .andExpect(jsonPath("$.createdAt").exists());

        ArgumentCaptor<HoldCreateCommand> captor = ArgumentCaptor.forClass(HoldCreateCommand.class);
        verify(holdService).createHold(captor.capture());

        HoldCreateCommand command = captor.getValue();
        assertEquals("req-1", command.requestId());
        assertEquals("admin1", command.actorId());
        assertEquals("settle-1", command.settlementId());
        assertEquals(HoldReasonCode.MANUAL_REVIEW, command.reasonCode());
        assertEquals("hold 요청", command.comment());
    }

    @Test
    @DisplayName("POST /api/admin/holds - settlementId blank면 400")
    void create_hold_with_blank_settlementId_should_return_400() throws Exception {
        given(sessionAuthProvider.getCurrentAdminId()).willReturn("admin1");

        String body = """
                {
                  "settlementId": "",
                  "reasonCode": "MANUAL_REVIEW",
                  "comment": "hold 요청"
                }
                """;

        mockMvc.perform(post("/api/admin/holds")
                        .session(adminSession())
                        .header("X-Request-Id", "req-blank-settlement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/admin/holds - reasonCode 누락이면 400")
    void create_hold_with_null_reasonCode_should_return_400() throws Exception {
        given(sessionAuthProvider.getCurrentAdminId()).willReturn("admin1");

        String body = """
                {
                  "settlementId": "settle-1",
                  "comment": "hold 요청"
                }
                """;

        mockMvc.perform(post("/api/admin/holds")
                        .session(adminSession())
                        .header("X-Request-Id", "req-null-reason")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/admin/holds - comment blank면 400")
    void create_hold_with_blank_comment_should_return_400() throws Exception {
        given(sessionAuthProvider.getCurrentAdminId()).willReturn("admin1");

        String body = """
                {
                  "settlementId": "settle-1",
                  "reasonCode": "MANUAL_REVIEW",
                  "comment": ""
                }
                """;

        mockMvc.perform(post("/api/admin/holds")
                        .session(adminSession())
                        .header("X-Request-Id", "req-blank-comment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /api/admin/holds/{holdId}/approve - 정상 200 + 최소 필드 반환")
    void approve_hold_should_return_200_with_minimum_fields() throws Exception {
        given(sessionAuthProvider.getCurrentAdminId()).willReturn("admin1");
        given(requestIdResolver.resolve(any())).willReturn("req-2");

        HoldDecisionResponse response = new HoldDecisionResponse(
                "req-2",
                HoldStatus.HOLD_ACTIVE,
                LocalDateTime.of(2026, 3, 20, 14, 10, 0)
        );

        given(holdService.approveHold(eq("hold-1"), any(HoldApproveCommand.class))).willReturn(response);

        String body = """
                {
                  "comment": "승인"
                }
                """;

        mockMvc.perform(patch("/api/admin/holds/{holdId}/approve", "hold-1")
                        .session(adminSession())
                        .header("X-Request-Id", "req-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("req-2"))
                .andExpect(jsonPath("$.status").value("HOLD_ACTIVE"))
                .andExpect(jsonPath("$.decidedAt").exists());

        ArgumentCaptor<HoldApproveCommand> captor = ArgumentCaptor.forClass(HoldApproveCommand.class);
        verify(holdService).approveHold(eq("hold-1"), captor.capture());

        HoldApproveCommand command = captor.getValue();
        assertEquals("req-2", command.requestId());
        assertEquals("admin1", command.actorId());
        assertEquals("승인", command.comment());
    }

    @Test
    @DisplayName("PATCH /api/admin/holds/{holdId}/release - 정상 200 + 최소 필드 반환")
    void release_hold_should_return_200_with_minimum_fields() throws Exception {
        given(sessionAuthProvider.getCurrentAdminId()).willReturn("admin1");
        given(requestIdResolver.resolve(any())).willReturn("req-3");

        HoldDecisionResponse response = new HoldDecisionResponse(
                "req-3",
                HoldStatus.RELEASED,
                LocalDateTime.of(2026, 3, 20, 14, 20, 0)
        );

        given(holdService.releaseHold(eq("hold-1"), any(HoldReleaseCommand.class))).willReturn(response);

        String body = """
                {
                  "comment": "해제"
                }
                """;

        mockMvc.perform(patch("/api/admin/holds/{holdId}/release", "hold-1")
                        .session(adminSession())
                        .header("X-Request-Id", "req-3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("req-3"))
                .andExpect(jsonPath("$.status").value("RELEASED"))
                .andExpect(jsonPath("$.decidedAt").exists());

        ArgumentCaptor<HoldReleaseCommand> captor = ArgumentCaptor.forClass(HoldReleaseCommand.class);
        verify(holdService).releaseHold(eq("hold-1"), captor.capture());

        HoldReleaseCommand command = captor.getValue();
        assertEquals("req-3", command.requestId());
        assertEquals("admin1", command.actorId());
        assertEquals("해제", command.comment());
    }

    @Test
    @DisplayName("PATCH /api/admin/holds/{holdId}/release - no-op 200이어도 최소 필드 반환")
    void release_hold_noop_should_still_return_minimum_fields() throws Exception {
        given(sessionAuthProvider.getCurrentAdminId()).willReturn("admin1");
        given(requestIdResolver.resolve(any())).willReturn("req-4");

        HoldDecisionResponse response = new HoldDecisionResponse(
                "req-4",
                HoldStatus.RELEASED,
                LocalDateTime.of(2026, 3, 20, 14, 30, 0)
        );

        given(holdService.releaseHold(eq("hold-1"), any(HoldReleaseCommand.class))).willReturn(response);

        mockMvc.perform(patch("/api/admin/holds/{holdId}/release", "hold-1")
                        .session(adminSession())
                        .header("X-Request-Id", "req-4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("req-4"))
                .andExpect(jsonPath("$.status").value("RELEASED"))
                .andExpect(jsonPath("$.decidedAt").exists());
    }

    @Test
    @DisplayName("PATCH /api/admin/holds/{holdId}/release - HOLD_NOT_ACTIVE면 409")
    void release_hold_when_not_active_should_return_409() throws Exception {
        given(sessionAuthProvider.getCurrentAdminId()).willReturn("admin1");
        given(requestIdResolver.resolve(any())).willReturn("req-5");

        given(holdService.releaseHold(eq("hold-1"), any(HoldReleaseCommand.class)))
                .willThrow(new ConflictException(
                        ReasonCode.HOLD_NOT_ACTIVE,
                        "release 가능한 hold 상태가 아닙니다."
                ));

        mockMvc.perform(patch("/api/admin/holds/{holdId}/release", "hold-1")
                        .session(adminSession())
                        .header("X-Request-Id", "req-5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET /api/admin/holds - 정상 200 + 목록 반환")
    void search_holds_should_return_200_with_items() throws Exception {
        AdminHoldQueueRowDto row = new AdminHoldQueueRowDto(
                "hold-1",
                "settle-1",
                "merchant-1",
                HoldStatus.HOLD_REQUESTED,
                HoldReasonCode.MANUAL_REVIEW,
                "검토 필요",
                LocalDateTime.of(2026, 3, 20, 15, 0, 0)
        );

        AdminHoldQueueResponse response = new AdminHoldQueueResponse(
                List.of(row),
                0,
                20,
                1L,
                1
        );

        given(holdQueryService.search(any(HoldQueueSearchRequestDto.class), any(Pageable.class)))
                .willReturn(response);

        mockMvc.perform(get("/api/admin/holds")
                        .session(adminSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].holdId").value("hold-1"))
                .andExpect(jsonPath("$.items[0].settlementId").value("settle-1"))
                .andExpect(jsonPath("$.items[0].merchantId").value("merchant-1"))
                .andExpect(jsonPath("$.items[0].status").value("HOLD_REQUESTED"))
                .andExpect(jsonPath("$.items[0].reasonCode").value("MANUAL_REVIEW"))
                .andExpect(jsonPath("$.items[0].requestedComment").value("검토 필요"))
                .andExpect(jsonPath("$.items[0].createdAt").exists())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @DisplayName("GET /api/admin/holds - query param을 HoldQueryService로 전달한다")
    void search_holds_should_pass_query_params_to_service() throws Exception {
        AdminHoldQueueResponse response = new AdminHoldQueueResponse(
                List.of(),
                0,
                20,
                0L,
                0
        );

        given(holdQueryService.search(any(HoldQueueSearchRequestDto.class), any(Pageable.class)))
                .willReturn(response);

        mockMvc.perform(get("/api/admin/holds")
                        .session(adminSession())
                        .param("status", "HOLD_ACTIVE")
                        .param("settlementId", "settle-1")
                        .param("merchantId", "merchant-1"))
                .andExpect(status().isOk());

        ArgumentCaptor<HoldQueueSearchRequestDto> requestCaptor =
                ArgumentCaptor.forClass(HoldQueueSearchRequestDto.class);
        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(holdQueryService, times(1))
                .search(requestCaptor.capture(), pageableCaptor.capture());

        HoldQueueSearchRequestDto request = requestCaptor.getValue();
        Pageable pageable = pageableCaptor.getValue();

        assertEquals(HoldStatus.HOLD_ACTIVE, request.status());
        assertEquals("settle-1", request.settlementId());
        assertEquals("merchant-1", request.merchantId());

        assertEquals(0, pageable.getPageNumber());
        assertEquals(20, pageable.getPageSize());
    }
}