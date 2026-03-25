package com.settleops.domain.settlement.api;

import com.settleops.domain.settlement.application.MerchantSettlementQueryService;
import com.settleops.domain.settlement.dto.MerchantSettlementDetailResponse;
import com.settleops.domain.settlement.dto.MerchantSettlementLineItemResponse;
import com.settleops.domain.settlement.dto.MerchantSettlementListItemResponse;
import com.settleops.domain.settlement.enums.SettlementLineType;
import com.settleops.domain.settlement.enums.SettlementStatus;
import com.settleops.global.audit.AuditLogger;
import com.settleops.global.auth.SessionAuthProvider;
import com.settleops.global.auth.controller.MeController;
import com.settleops.global.auth.resolver.LoginMerchantArgumentResolver;
import com.settleops.global.error.ForbiddenException;
import com.settleops.global.error.GlobalExceptionHandler;
import com.settleops.global.error.NotFoundException;
import com.settleops.global.error.UnauthorizedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MerchantSettlementQueryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, LoginMerchantArgumentResolver.class})
@ActiveProfiles("test")
class MerchantSettlementQueryControllerWebMvcTest {

    private static final String MERCHANT_ID = "MERCHANT_1001";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MerchantSettlementQueryService merchantSettlementQueryService;

    @MockitoBean
    private SessionAuthProvider sessionAuthProvider;

    @MockitoBean
    private AuditLogger auditLogger;

    @Test
    @DisplayName("Merchant 정산 리스트 조회 시 세션이 없으면 401을 반환한다")
    void getMerchantSettlements_returns_unauthorized_when_session_missing() throws Exception {
        when(sessionAuthProvider.getCurrentMerchantId())
                .thenThrow(new UnauthorizedException("로그인이 필요합니다."));

        mockMvc.perform(get("/api/merchants/{merchantId}/settlements", MERCHANT_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.reason").doesNotExist())
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));

        verifyNoInteractions(merchantSettlementQueryService);
    }

    @Test
    @DisplayName("Merchant 정산 리스트 조회 시 로그인 merchantId와 path merchantId가 다르면 403을 반환한다")
    void getMerchantSettlements_returns_forbidden_when_merchant_mismatch() throws Exception {
        when(sessionAuthProvider.getCurrentMerchantId())
                .thenThrow(new ForbiddenException("접근 권한이 없습니다."));

        mockMvc.perform(get("/api/merchants/{merchantId}/settlements", "OTHER_MERCHANT")
                        .sessionAttr(MeController.SessionKeys.ROLE, "MERCHANT")
                        .sessionAttr(MeController.SessionKeys.MERCHANT_ID, MERCHANT_ID)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.reason").doesNotExist())
                .andExpect(jsonPath("$.message").value("접근 권한이 없습니다."));

        verifyNoInteractions(merchantSettlementQueryService);
    }

    @Test
    @DisplayName("Merchant 정산 상세 조회 시 존재하지 않는 settlementId면 404를 반환한다")
    void getMerchantSettlementDetail_returns_not_found_when_settlement_not_found() throws Exception {
        when(sessionAuthProvider.getCurrentMerchantId()).thenReturn(MERCHANT_ID);
        when(merchantSettlementQueryService.getMerchantSettlementDetail(
                eq(MERCHANT_ID),
                eq(MERCHANT_ID),
                eq("missing-settlement")
        )).thenThrow(new NotFoundException("settlement not found"));

        mockMvc.perform(get("/api/merchants/{merchantId}/settlements/{settlementId}",
                        MERCHANT_ID,
                        "missing-settlement")
                        .sessionAttr(MeController.SessionKeys.ROLE, "MERCHANT")
                        .sessionAttr(MeController.SessionKeys.MERCHANT_ID, MERCHANT_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.reason").doesNotExist())
                .andExpect(jsonPath("$.message").value("settlement not found"));

        verify(merchantSettlementQueryService).getMerchantSettlementDetail(
                MERCHANT_ID,
                MERCHANT_ID,
                "missing-settlement"
        );
    }

    @Test
    @DisplayName("Merchant 정산 상세 조회 시 다른 merchant의 settlement이면 403을 반환한다")
    void getMerchantSettlementDetail_returns_forbidden_when_settlement_owner_does_not_match() throws Exception {
        when(sessionAuthProvider.getCurrentMerchantId())
                .thenThrow(new ForbiddenException("접근 권한이 없습니다."));

        mockMvc.perform(get("/api/merchants/{merchantId}/settlements/{settlementId}",
                        "OTHER_MERCHANT",
                        "settlement-1")
                        .sessionAttr(MeController.SessionKeys.ROLE, "MERCHANT")
                        .sessionAttr(MeController.SessionKeys.MERCHANT_ID, MERCHANT_ID))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.reason").doesNotExist())
                .andExpect(jsonPath("$.message").value("접근 권한이 없습니다."));

        verifyNoInteractions(merchantSettlementQueryService);
    }

    @Test
    @DisplayName("Merchant 정산 상세 조회 성공 시 200과 상세 응답을 반환한다")
    void getMerchantSettlementDetail_success() throws Exception {
        MerchantSettlementDetailResponse detail = new MerchantSettlementDetailResponse(
                "settlement-1",
                LocalDate.of(2026, 3, 10),
                SettlementStatus.READY,
                10000L,
                1000L,
                100L,
                8900L,
                List.of(
                        new MerchantSettlementLineItemResponse(
                                "1",
                                SettlementLineType.PAYMENT,
                                "payment-1",
                                10000L
                        )
                )
        );

        when(sessionAuthProvider.getCurrentMerchantId()).thenReturn(MERCHANT_ID);
        when(merchantSettlementQueryService.getMerchantSettlementDetail(
                eq(MERCHANT_ID),
                eq(MERCHANT_ID),
                eq("settlement-1")
        )).thenReturn(detail);

        mockMvc.perform(get("/api/merchants/{merchantId}/settlements/{settlementId}",
                        MERCHANT_ID,
                        "settlement-1")
                        .sessionAttr(MeController.SessionKeys.ROLE, "MERCHANT")
                        .sessionAttr(MeController.SessionKeys.MERCHANT_ID, MERCHANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.settlementId").value("settlement-1"))
                .andExpect(jsonPath("$.baseDate").value("2026-03-10"))
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.gross").value(10000))
                .andExpect(jsonPath("$.fee").value(1000))
                .andExpect(jsonPath("$.vat").value(100))
                .andExpect(jsonPath("$.net").value(8900))
                .andExpect(jsonPath("$.lines.length()").value(1))
                .andExpect(jsonPath("$.lines[0].settlementLineId").value("1"))
                .andExpect(jsonPath("$.lines[0].type").value("PAYMENT"))
                .andExpect(jsonPath("$.lines[0].paymentId").value("payment-1"))
                .andExpect(jsonPath("$.lines[0].amount").value(10000));

        verify(merchantSettlementQueryService).getMerchantSettlementDetail(
                MERCHANT_ID,
                MERCHANT_ID,
                "settlement-1"
        );
    }

    @Test
    @DisplayName("Merchant 정산 리스트 조회 성공 시 status/from/to 필터를 포함해 200과 페이지 응답을 반환한다")
    void getMerchantSettlements_success() throws Exception {
        MerchantSettlementListItemResponse item1 = new MerchantSettlementListItemResponse(
                "settlement-1",
                LocalDate.of(2026, 3, 10),
                SettlementStatus.READY,
                10000L,
                1000L,
                100L,
                8900L,
                LocalDateTime.of(2026, 3, 11, 10, 0)
        );

        when(sessionAuthProvider.getCurrentMerchantId()).thenReturn(MERCHANT_ID);
        when(merchantSettlementQueryService.getMerchantSettlements(
                eq(MERCHANT_ID),
                eq(MERCHANT_ID),
                eq(SettlementStatus.READY),
                eq(LocalDate.of(2026, 3, 1)),
                eq(LocalDate.of(2026, 3, 31)),
                eq(PageRequest.of(0, 20))
        )).thenReturn(new PageImpl<>(List.of(item1), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/merchants/{merchantId}/settlements", MERCHANT_ID)
                        .sessionAttr(MeController.SessionKeys.ROLE, "MERCHANT")
                        .sessionAttr(MeController.SessionKeys.MERCHANT_ID, MERCHANT_ID)
                        .param("status", "READY")
                        .param("from", "2026-03-01")
                        .param("to", "2026-03-31")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].settlementId").value("settlement-1"))
                .andExpect(jsonPath("$.content[0].baseDate").value("2026-03-10"))
                .andExpect(jsonPath("$.content[0].status").value("READY"))
                .andExpect(jsonPath("$.content[0].gross").value(10000))
                .andExpect(jsonPath("$.content[0].fee").value(1000))
                .andExpect(jsonPath("$.content[0].vat").value(100))
                .andExpect(jsonPath("$.content[0].net").value(8900))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(merchantSettlementQueryService).getMerchantSettlements(
                MERCHANT_ID,
                MERCHANT_ID,
                SettlementStatus.READY,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                PageRequest.of(0, 20)
        );
    }
}