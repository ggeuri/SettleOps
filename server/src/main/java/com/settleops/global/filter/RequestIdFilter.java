package com.settleops.global.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 🚨 requestId는 시스템 전역에서 본 Filter에서만 생성/주입한다.
 * 다른 계층(Interceptor, AOP 등)에서 중복 생성 금지
 * */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String REQUEST_ID = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        // 1. 헤더에서 기존 ID가 있는지 확인, 없으면 새로 생성
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        // 2. MDC에 저장 (로그에 찍히도록 설정)
        MDC.put(REQUEST_ID,requestId);

        // 3. 응답 헤더에 추가 (클라이언트 확인용)
        // ✅ 모든 API 응답은 반드시 X-Request-Id 헤더를 포함해야 한다.
        response.setHeader(REQUEST_ID_HEADER, requestId);

        // 4. QueryString 포함 full URI 구성
        String fullUri = request.getRequestURI() +
                (request.getQueryString() != null
                        ? "?" + request.getQueryString()
                        : "");


        try {
            log.info(">>> {} {}", request.getMethod(), fullUri);
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            log.info("<<< {}ms status={}", duration, response.getStatus());

            // 5. MDC 정리
            MDC.remove(REQUEST_ID);
        }
    }
}
