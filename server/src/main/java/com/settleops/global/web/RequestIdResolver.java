package com.settleops.global.web;

import com.settleops.global.logging.RequestIdKeys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import static com.settleops.global.logging.RequestIdKeys.HEADER;

/**
 * HttpServletRequest 에서 requestId 를 표준 방식으로 추출한다.
 * <p>requestId 는 RequestIdFilter 가 request attribute 에 주입한 값을 사용한다.</p>
 */
@Component
public class RequestIdResolver {

    public String resolve(HttpServletRequest request) {
        Object attr = request.getAttribute(RequestIdKeys.ATTR_KEY);
        if (attr instanceof String s && !s.isBlank()) {
            return s; // 1순위: Filter가 주입한 attribute
        }

        // 2순위: 테스트/특수상황에서 attribute가 없을 수 있으니 header fallback
        String header = request.getHeader(HEADER);
        if (header != null && !header.isBlank()) {
            return header;
        }

        // Filter 단일 책임 계약: requestId는 Filter가 보장한다.
        // 여기서 500(IllegalStateException) 만들지 않는다. (없으면 호출부에서 정책적으로 처리)
        return null;
    }
}