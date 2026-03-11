package com.settleops.global.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.settleops.global.logging.RequestIdKeys.HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RequestIdFilterTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
                .standaloneSetup(new DummyController())
                .addFilters(new RequestIdFilter())
                .build();
    }

    @Test
    void whenHeaderMissing_thenResponseHasXRequestId() throws Exception {
        var result = mockMvc.perform(get("/__request-id-smoke"))
                .andExpect(status().isOk())
                .andReturn();

        String requestId = result.getResponse().getHeader(HEADER);
        assertThat(requestId).isNotBlank();
    }

    @Test
    void whenHeaderProvided_thenEchoSameXRequestId() throws Exception {
        var result = mockMvc.perform(get("/__request-id-smoke")
                        .header(HEADER, "req-test-123"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getHeader(HEADER))
                .isEqualTo("req-test-123");
    }

    @RestController
    static class DummyController {
        @GetMapping("/__request-id-smoke")
        public String ok() {
            return "ok";
        }
    }
}