package com.settleops.domain.settlement.api;

import com.settleops.domain.settlement.application.MerchantSettlementQueryService;
import com.settleops.domain.settlement.dto.MerchantSettlementDetailResponse;
import com.settleops.domain.settlement.dto.MerchantSettlementLineItemResponse;
import com.settleops.domain.settlement.dto.MerchantSettlementListItemResponse;
import com.settleops.domain.settlement.enums.SettlementLineType;
import com.settleops.domain.settlement.enums.SettlementStatus;
import com.settleops.global.audit.AuditLogger;
import com.settleops.global.auth.MerchantSessionResolver;
import com.settleops.global.error.ForbiddenException;
import com.settleops.global.error.GlobalExceptionHandler;
import com.settleops.global.error.NotFoundException;
import com.settleops.global.error.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MerchantSettlementQueryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class MerchantSettlementQueryControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MerchantSettlementQueryService merchantSettlementQueryService;

    @MockitoBean
    private MerchantSessionResolver merchantSessionResolver;

    @MockitoBean
    private AuditLogger auditLogger;

    @Test
    @DisplayName("Merchant 정산 리스트 조회 시 세션이 없으면 401을 반환한다")
    void getMerchantSettlements_unauthorizedWhenSessionMissing() throws Exception {
        when(merchantSessionResolver.resolveMerchantId(any(HttpServletRequest.class)))
                .thenThrow(new UnauthorizedException("login required"));

        mockMvc.perform(get("/api/merchants/{merchantId}/settlements", "merchant-1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.reason").isEmpty())
                .andExpect(jsonPath("$.message").value("login required"));
    }

    @Test
    @DisplayName("Merchant 정산 리스트 조회 시 로그인 merchantId와 path merchantId가 다르면 403을 반환한다")
    void getMerchantSettlements_forbiddenWhenMerchantMismatch() throws Exception {
        when(merchantSessionResolver.resolveMerchantId(any(HttpServletRequest.class)))
                .thenReturn("merchant-1");

        when(merchantSettlementQueryService.getMerchantSettlements(
                eq("merchant-1"),
                eq("merchant-2"),
                any()
        )).thenThrow(new ForbiddenException("merchant mismatch"));

        mockMvc.perform(get("/api/merchants/{merchantId}/settlements", "merchant-2"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.reason").isEmpty())
                .andExpect(jsonPath("$.message").value("merchant mismatch"));
    }

    @Test
    @DisplayName("Merchant 정산 상세 조회 시 존재하지 않는 settlementId면 404를 반환한다")
    void getMerchantSettlementDetail_notFoundWhenSettlementDoesNotExist() throws Exception {
        when(merchantSessionResolver.resolveMerchantId(any(HttpServletRequest.class)))
                .thenReturn("merchant-1");

        when(merchantSettlementQueryService.getMerchantSettlementDetail(
                "merchant-1",
                "merchant-1",
                "settlement-404"
        )).thenThrow(new NotFoundException("settlement not found"));

        mockMvc.perform(get("/api/merchants/{merchantId}/settlements/{settlementId}", "merchant-1", "settlement-404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.reason").doesNotExist())
                .andExpect(jsonPath("$.message").value("settlement not found"));
    }

    @Test
    @DisplayName("Merchant 정산 상세 조회 시 다른 merchant의 settlement이면 403을 반환한다")
    void getMerchantSettlementDetail_forbiddenWhenSettlementOwnerDoesNotMatch() throws Exception {
        when(merchantSessionResolver.resolveMerchantId(any(HttpServletRequest.class)))
                .thenReturn("merchant-1");

        when(merchantSettlementQueryService.getMerchantSettlementDetail(
                "merchant-1",
                "merchant-1",
                "settlement-1"
        )).thenThrow(new ForbiddenException("merchant mismatch"));

        mockMvc.perform(get("/api/merchants/{merchantId}/settlements/{settlementId}", "merchant-1", "settlement-1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.reason").doesNotExist())
                .andExpect(jsonPath("$.message").value("merchant mismatch"));
    }

    @Test
    @DisplayName("Merchant 정산 상세 조회 성공 시 200과 상세 응답을 반환한다")
    void getMerchantSettlementDetail_success() throws Exception {
        MerchantSettlementDetailResponse response = new MerchantSettlementDetailResponse(
                "settlement-1",
                LocalDate.of(2026, 3, 10),
                SettlementStatus.READY,
                10000L,
                0L,
                0L,
                10000L,
                List.of(
                        new MerchantSettlementLineItemResponse(
                                "1",
                                SettlementLineType.PAYMENT,
                                "payment-1",
                                10000L
                        )
                )
        );

        when(merchantSessionResolver.resolveMerchantId(any(HttpServletRequest.class)))
                .thenReturn("merchant-1");

        when(merchantSettlementQueryService.getMerchantSettlementDetail(
                "merchant-1",
                "merchant-1",
                "settlement-1"
        )).thenReturn(response);

        mockMvc.perform(get("/api/merchants/{merchantId}/settlements/{settlementId}", "merchant-1", "settlement-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settlementId").value("settlement-1"))
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.lines[0].paymentId").value("payment-1"));
    }

    @Test
    @DisplayName("Merchant 정산 리스트 조회 성공 시 200과 페이지 응답을 반환한다")
    void getMerchantSettlements_success() throws Exception {
        MerchantSettlementListItemResponse item = new MerchantSettlementListItemResponse(
                "settlement-1",
                LocalDate.of(2026, 3, 10),
                SettlementStatus.READY,
                10000L,
                0L,
                0L,
                10000L,
                LocalDateTime.of(2026, 3, 11, 10, 0)
        );

        when(merchantSessionResolver.resolveMerchantId(any(HttpServletRequest.class)))
                .thenReturn("merchant-1");

        when(merchantSettlementQueryService.getMerchantSettlements(
                eq("merchant-1"),
                eq("merchant-1"),
                any()
        )).thenReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/merchants/{merchantId}/settlements", "merchant-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].settlementId").value("settlement-1"))
                .andExpect(jsonPath("$.content[0].status").value("READY"));
    }
}