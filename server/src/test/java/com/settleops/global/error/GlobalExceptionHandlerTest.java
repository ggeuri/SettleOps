package com.settleops.global.error;

import com.settleops.global.enums.ReasonCode;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandlerTest.TestController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MDC.put("requestId", "test-request-id"); // 테스트용 requestId
    }

    @AfterEach
    void tearDown() {
        MDC.clear(); // 테스트 끝난 후 반드시 clear
    }

    @RestController
    static class TestController {

        @GetMapping("/test/409")
        public String conflict() {
            throw new ConflictException(
                    ReasonCode.PAYMENT_NOT_CAPTURED,
                    "결제 상태가 올바르지 않습니다."
            );
        }

        @GetMapping("/test/403")
        public String forbidden() {
            throw new ForbiddenException("권한이 없습니다.");
        }

        @GetMapping("/test/500")
        public String serverError() {
            throw new RuntimeException("강제 500");
        }
    }

    @Test
    void should_return_409_rule_violation_format() throws Exception {
        mockMvc.perform(get("/test/409"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RULE_VIOLATION"))
                .andExpect(jsonPath("$.reason").value("PAYMENT_NOT_CAPTURED"))
                .andExpect(jsonPath("$.requestId").value("test-request-id"));
    }

    @Test
    void should_return_403_forbidden_format() throws Exception {
        mockMvc.perform(get("/test/403"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.reason").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.requestId").value("test-request-id"));
    }

    @Test
    void should_return_500_internal_error_format() throws Exception {
        mockMvc.perform(get("/test/500"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.reason").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.message")
                        .value("시스템 오류가 발생했습니다. 관리자에게 문의하세요."))
                .andExpect(jsonPath("$.requestId").value("test-request-id"));
    }
}