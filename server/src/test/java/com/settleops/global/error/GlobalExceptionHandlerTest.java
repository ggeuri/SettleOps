package com.settleops.global.error;

import com.settleops.global.enums.ReasonCode;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false) // ✅ Security/Filter 영향 제거: “예외 포맷”만 검증
@Import(GlobalExceptionHandlerTest.TestController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mockMvc;

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

        @GetMapping("/test/401")
        public String unauthorized() {
            throw new UnauthorizedException("UNAUTHORIZED");
        }


        @GetMapping("/test/500")
        public String serverError() {
            throw new RuntimeException("강제 500");
        }
    }

    /**
     * 409 Conflict → code=RULE_VIOLATION, reason 필수
     * requestId는 “헤더에만” 제공하므로 바디에는 없어야 함
     */
    @Test
    void should_return_409_rule_violation_format() throws Exception {
        mockMvc.perform(get("/test/409"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RULE_VIOLATION"))
                .andExpect(jsonPath("$.reason").value("PAYMENT_NOT_CAPTURED"))
                .andExpect(jsonPath("$.message").value("결제 상태가 올바르지 않습니다."))
                .andExpect(jsonPath("$.requestId").doesNotExist());
    }

    /**
     * 403 Forbidden → code=FORBIDDEN, reason 사용 금지(null 고정)
     * requestId는 “헤더에만” 제공하므로 바디에는 없어야 함
     */
    @Test
    void should_return_403_forbidden_format() throws Exception {
        mockMvc.perform(get("/test/403"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.reason").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.message").value("권한이 없습니다."))
                .andExpect(jsonPath("$.requestId").doesNotExist());
    }

    /**
     * 401 Unauthorized → code=UNAUTHORIZED, reason 사용 금지(null 고정)
     * requestId는 “헤더에만” 제공하므로 바디에는 없어야 함
     */
    @Test
    void should_return_401_unauthorized_format() throws Exception {
        mockMvc.perform(get("/test/401"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.reason").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.message").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.requestId").doesNotExist());
    }


    /**
     * 500 Internal Server Error → code=INTERNAL_SERVER_ERROR, reason=null
     * message는 표준 문구로 내려가야 함
     * requestId는 “헤더에만” 제공하므로 바디에는 없어야 함
     */
    @Test
    void should_return_500_internal_error_format() throws Exception {
        mockMvc.perform(get("/test/500"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.reason").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.message").value("시스템 오류가 발생했습니다. 관리자에게 문의하세요."))
                .andExpect(jsonPath("$.requestId").doesNotExist());
    }
}