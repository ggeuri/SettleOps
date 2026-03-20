package com.settleops.global.auth.controller;

import com.settleops.global.audit.AuditLogger;
import com.settleops.global.auth.SessionAuthProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MeController.class)
@AutoConfigureMockMvc(addFilters = false)
class MeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SessionAuthProvider sessionAuthProvider;

    @MockBean
    private AuditLogger auditLogger;

    @Test
    @DisplayName("세션에 role이 없으면 /api/me 는 401을 반환한다")
    void me_shouldReturn401_whenRoleDoesNotExist() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(get("/api/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("admin 세션이면 /api/me 에 role과 adminId를 반환한다")
    void me_shouldReturnAdminInfo_whenAdminSessionExists() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(MeController.SessionKeys.ROLE, "ADMIN");
        session.setAttribute(MeController.SessionKeys.ADMIN_ID, "ADMIN_1001");

        mockMvc.perform(get("/api/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.buyerId").value(nullValue()))
                .andExpect(jsonPath("$.merchantId").value(nullValue()))
                .andExpect(jsonPath("$.adminId").value("ADMIN_1001"));
    }

    @Test
    @DisplayName("consumer 세션이면 /api/me 에 role과 buyerId를 반환한다")
    void me_shouldReturnConsumerInfo_whenConsumerSessionExists() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(MeController.SessionKeys.ROLE, "CONSUMER");
        session.setAttribute(MeController.SessionKeys.BUYER_ID, "BUYER_1001");

        mockMvc.perform(get("/api/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CONSUMER"))
                .andExpect(jsonPath("$.buyerId").value("BUYER_1001"))
                .andExpect(jsonPath("$.merchantId").value(nullValue()))
                .andExpect(jsonPath("$.adminId").value(nullValue()));
    }
}