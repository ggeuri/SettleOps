package com.settleops.domain.admin.trace;

import com.settleops.domain.admin.trace.controller.AuditTraceController;
import com.settleops.domain.admin.trace.dto.AuditTraceResponseDto;
import com.settleops.domain.admin.trace.dto.AuditTraceRowDto;
import com.settleops.domain.admin.trace.dto.AuditTraceSearchRequestDto;
import com.settleops.domain.admin.trace.service.AuditTraceService;
import com.settleops.global.audit.ActorType;
import com.settleops.global.audit.AuditLog;
import com.settleops.global.audit.EntityType;
import com.settleops.global.enums.Action;
import com.settleops.global.error.BadRequestException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuditTraceController.class)
class AuditTraceControllerTest {
    @Autowired
    MockMvc mockMvc;
    @MockitoBean
    AuditTraceService auditTraceService;

    //requestId/merchantId 둘 다 없으면 400
    @Test
    @WithMockUser(roles = "ADMIN")
    void requestId_and_merchantId_missing_should_return_400() throws Exception {
        // Given-When-Then
        // Given: service가 "requestId 또는 merchantId는 필수입니다." 라는 400 예외를 던지도록 세팅
        given(auditTraceService.searchAuditTraces(any(), any()))
                .willThrow(new BadRequestException("requestId 또는 merchantId는 필수입니다."));

        // When: requestId/merchantId 없이 조회 호출하면
        mockMvc.perform(get("/api/admin/audit-logs"))

                // Then: 400 + ErrorResponse 규격 검증해라
                .andExpect(status().isBadRequest())
                // jsonPath jsonPath("$.code")
                //($ = JSON 전체, .code = 그 안의 code) = 바디의 code도 규격대로 BAD_REQUEST인지 확인
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.reason").value(nullValue()))
                .andExpect(jsonPath("$.message").value("requestId 또는 merchantId는 필수입니다."));
    }

    //둘 다 오면 requestId 우선 (merchantId 무시)
    @Test
    @WithMockUser(roles = "ADMIN")
    void requestId_and_merchantId_both_given_should_prioritize_requestId() throws Exception {
        // Given: 서비스가 requestId="R1"로 응답했다고 가정
        var response = new AuditTraceResponseDto(
                "R1",
                List.of(),
                0,
                20,
                0L,
                0
        );
        given(auditTraceService.searchAuditTraces(any(),any()))
                .willReturn(response);

        // When: requestId + merchantId + from/to 같이 보냄
        mockMvc.perform(get("/api/admin/audit-logs")
                .param("requestId","R1")
                .param("merchantId","M1")
                .param("from", "2026-02-20T00:00:00")
                .param("to", "2026-02-25T00:00:00"))

        // Then: 200 + response.requestId == R1인지 검증할 것
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("R1"));

        // 그리고 컨트롤러가 서비스에 넘긴 rq가 requestId=R1인지 확인

        ArgumentCaptor<AuditTraceSearchRequestDto> captor =
                ArgumentCaptor.forClass(AuditTraceSearchRequestDto.class);

        //rq는 capture
        verify(auditTraceService, times(1)).searchAuditTraces(captor.capture(), any());

        AuditTraceSearchRequestDto rq = captor.getValue();

        assertEquals("R1",rq.getRequestId());
        assertEquals("M1", rq.getMerchantId());
        assertEquals(LocalDateTime.parse("2026-02-20T00:00:00"), rq.getFrom());
        assertEquals(LocalDateTime.parse("2026-02-25T00:00:00"), rq.getTo());

    }

    //3. requestId 검색은 기간(from/to) 없어도 200
    @Test
    @WithMockUser(roles = "ADMIN")
    void requestId_search_without_from_to_should_return_200() throws Exception{

//        (requestId="R1", items=List.of(…dummy 1개…), page/size/total…)
        AuditLog log = AuditLog.builder()
                .occurredAt(LocalDateTime.parse("2026-02-25T00:00:00"))
                .actorType(ActorType.ADMIN)
                .actorId("admin1")
                .action(Action.HOLD_APPROVED)
                .entityType(EntityType.HOLD)
                .entityId("E1")
                .statusBefore(null)
                .statusAfter(null)
                .metaJson("{}")
                .merchantId("M1")
                .build();

        AuditTraceRowDto row = AuditTraceRowDto.from(log);

        var response = new AuditTraceResponseDto(
                "R1",
                List.of(row),
                0,
                20,
                1L,
                1
        );

        given(auditTraceService.searchAuditTraces(any(),any()))
                .willReturn(response);

        // When: requestId + merchantId + from/to 같이 안보냄
        mockMvc.perform(get("/api/admin/audit-logs")
                .param("requestId","R1"))

                // Then: 200 , items 반환
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("R1"))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].actorId").value("admin1"))
                .andExpect(jsonPath("$.items[0].merchantId").value("M1"));

        // 그리고 컨트롤러가 서비스에 넘긴 rq의 from/to가 null인지 확인, requestId=R1인지 확인
        ArgumentCaptor<AuditTraceSearchRequestDto> captor =
                ArgumentCaptor.forClass(AuditTraceSearchRequestDto.class);

        //rq는 capture
        verify(auditTraceService, times(1)).searchAuditTraces(captor.capture(), any());

        AuditTraceSearchRequestDto rq = captor.getValue();
        assertEquals("R1", rq.getRequestId());
        assertNull(rq.getFrom());
        assertNull(rq.getTo());
    }


    // 페이징 기본 계약 (size=20) , 정렬은 Repository(QueryDSL)
    @Test
    @WithMockUser(roles = "ADMIN")
    void pageable_default_should_be_size20_and_sort_occurredAt_desc() throws Exception {
        // Given
        var response = new AuditTraceResponseDto(
                "R1",
                List.of(),
                0,
                20,
                0L,
                0
        );

        given(auditTraceService.searchAuditTraces(any(), any()))
                .willReturn(response);

        // When
        mockMvc.perform(get("/api/admin/audit-logs")
                        .param("requestId", "R1"))
                .andExpect(status().isOk());

        // Then: pageable 캡처
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(auditTraceService, times(1)).searchAuditTraces(any(), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();

        assertEquals(0, pageable.getPageNumber());
        assertEquals(20, pageable.getPageSize());

    }

    // 응답 DTO 필드 계약(Trace 6컬럼 + 확장필드) 고정
    @Test
    @WithMockUser(roles = "ADMIN")
    void response_items_should_contain_fixed_fields_and_metaJson_not_null() throws Exception {
        // Given: metaJson이 비어있어도 "{}"로 수렴해야 함
        // items[n]에 아래 필드가 항상 존재
        AuditLog log = AuditLog.builder()
                .requestId("R1")
                .occurredAt(LocalDateTime.parse("2026-02-25T00:00:00"))
                .actorType(ActorType.ADMIN)
                .actorId("admin1")
                .action(Action.HOLD_APPROVED)
                .entityType(EntityType.HOLD)
                .entityId("E1")
                .statusBefore(null)
                .statusAfter(null)
                .metaJson("")          // 빈 값
                .merchantId("M1")
                .build();

        Field f = AuditLog.class.getDeclaredField("auditId");
        f.setAccessible(true);
        f.set(log, 1L);

        AuditTraceRowDto row = AuditTraceRowDto.from(log);

        var response = new AuditTraceResponseDto(
                "R1",
                List.of(row),
                0,
                20,
                1L,
                1
        );

        given(auditTraceService.searchAuditTraces(any(), any()))
                .willReturn(response);

        // When / Then
        mockMvc.perform(get("/api/admin/audit-logs")
                        .param("requestId", "R1"))
                .andExpect(status().isOk())
                // 필드 존재(고정 컬럼)
                .andExpect(jsonPath("$.items[0].occurredAt").exists())
                .andExpect(jsonPath("$.items[0].actorType").exists())
                .andExpect(jsonPath("$.items[0].actorId").exists())
                .andExpect(jsonPath("$.items[0].action").exists())
                .andExpect(jsonPath("$.items[0].entityType").exists())
                .andExpect(jsonPath("$.items[0].entityId").exists())
                // metaJson은 null 금지 + 빈 값은 "{}"
                .andExpect(jsonPath("$.items[0].metaJson").value("{}"))
                // 확장필드 존재
                .andExpect(jsonPath("$.items[0].auditId").value(1))
                .andExpect(jsonPath("$.items[0].merchantId").exists());
    }


}