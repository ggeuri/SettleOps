package com.settleops.domain.admin.trace;

import com.settleops.domain.admin.trace.controller.AuditTraceController;
import com.settleops.domain.admin.trace.dto.AuditTraceResponseDto;
import com.settleops.domain.admin.trace.dto.AuditTraceSearchRequestDto;
import com.settleops.domain.admin.trace.service.AuditTraceService;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
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
        verify(auditTraceService).searchAuditTraces(captor.capture(), any());

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


    }

}