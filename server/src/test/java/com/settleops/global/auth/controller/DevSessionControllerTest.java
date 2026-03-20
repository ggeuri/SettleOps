package com.settleops.global.auth.controller;

import com.settleops.global.audit.AuditLogger;
import com.settleops.global.auth.SessionAuthProvider;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DevSessionController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("local")
@TestPropertySource(properties = "dev-session.enabled=true")
class DevSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SessionAuthProvider sessionAuthProvider;

    @MockBean
    private AuditLogger auditLogger;

    @Test
    @DisplayName("admin 로그인 시 ROLE과 ADMIN_ID를 세션에 저장하고 기존 사용자 세션 값은 제거한다")
    void loginAdmin_shouldSetAdminSession_andClearOtherSessionValues() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(MeController.SessionKeys.ROLE, "CONSUMER");
        session.setAttribute(MeController.SessionKeys.BUYER_ID, "BUYER_1001");
        session.setAttribute(MeController.SessionKeys.MERCHANT_ID, "MERCHANT_1001");

        MvcResult result = mockMvc.perform(post("/api/dev/login-admin")
                        .param("adminId", "ADMIN_1001")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        HttpSession updatedSession = result.getRequest().getSession(false);

        assertThat(updatedSession).isNotNull();
        assertThat(updatedSession.getAttribute(MeController.SessionKeys.ROLE)).isEqualTo("ADMIN");
        assertThat(updatedSession.getAttribute(MeController.SessionKeys.ADMIN_ID)).isEqualTo("ADMIN_1001");
        assertThat(updatedSession.getAttribute(MeController.SessionKeys.BUYER_ID)).isNull();
        assertThat(updatedSession.getAttribute(MeController.SessionKeys.MERCHANT_ID)).isNull();
    }

    @Test
    @DisplayName("adminId가 blank면 400을 반환한다")
    void loginAdmin_shouldReturnBadRequest_whenAdminIdIsBlank() throws Exception {
        mockMvc.perform(post("/api/dev/login-admin")
                        .param("adminId", " "))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("logout 호출 시 세션을 무효화한다")
    void logout_shouldInvalidateSession() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(MeController.SessionKeys.ROLE, "ADMIN");
        session.setAttribute(MeController.SessionKeys.ADMIN_ID, "ADMIN_1001");

        MvcResult result = mockMvc.perform(post("/api/dev/logout").session(session))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getRequest().getSession(false)).isNull();
    }
}