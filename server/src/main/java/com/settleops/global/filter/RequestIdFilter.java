package com.settleops.global.filter;

import jakarta.servlet.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter implements Filter {
    private static final String REQUEST_ID = "request_id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        // 1. UUID 생성 (이름표 만들기)
        String requestId = UUID.randomUUID().toString().substring(0, 8);

        // 2. MDC에 저장 (로그에 찍히도록 설정)
        MDC.put(REQUEST_ID,requestId);
        log.info("request start");
        try {
            chain.doFilter(request, response);
        } finally {
            // 3. 요청이 끝나면 비워주기 (중요: 쓰레드 풀을 사용하므로 지워야 함)
            MDC.clear();
        }
    }
}
