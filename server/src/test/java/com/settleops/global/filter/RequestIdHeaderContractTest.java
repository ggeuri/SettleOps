package com.settleops.global.filter;

import com.settleops.global.audit.AuditLogger;
import com.settleops.global.error.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.settleops.global.logging.RequestIdKeys.HEADER;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RequestIdHeaderContractTest {

    private final AuditLogger auditLogger = mock(AuditLogger.class);

    @RestController
    static class TestController {
        @GetMapping("/ok")
        String ok() { return "ok"; }

        @GetMapping("/boom")
        String boom() { throw new RuntimeException("boom"); }
    }

    private MockMvc mockMvc() {
        return MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler(auditLogger))
                .addFilters(new RequestIdFilter())
                .build();
    }

    @Test
    void should_generate_request_id_header_when_missing() throws Exception {
        MockMvc mvc = mockMvc();

        mvc.perform(get("/ok"))
                .andExpect(status().isOk())
                .andExpect(header().exists(HEADER))
                .andExpect(header().string(HEADER, not(isEmptyOrNullString())));
    }

    @Test
    void should_echo_request_id_header_when_provided() throws Exception {
        MockMvc mvc = mockMvc();

        mvc.perform(get("/ok").header(HEADER, "test-request-id"))
                .andExpect(status().isOk())
                .andExpect(header().string(HEADER, "test-request-id"));
    }

    @Test
    void should_still_include_request_id_header_on_exception() throws Exception {
        MockMvc mvc = mockMvc();

        mvc.perform(get("/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(header().exists(HEADER))
                .andExpect(jsonPath("$.requestId").doesNotExist());
    }
}