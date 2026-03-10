package com.settleops.global.web;

import com.settleops.global.error.BadRequestException;
import com.settleops.global.logging.RequestIdKeys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * HttpServletRequest 에서 requestId 를 표준 방식으로 추출한다.
 *
 * <p>requestId 는 RequestIdFilter 가 request attribute 에 주입한 값을 사용한다.</p>
 * <p>누락 또는 공백이면 400(BAD_REQUEST)로 처리한다.</p>
 */
@Component
public class RequestIdResolver {

    public String resolve(HttpServletRequest request) {
        String requestId = (String) request.getAttribute(RequestIdKeys.ATTR_KEY);

        if (requestId == null || requestId.isBlank()) {
            throw new BadRequestException("X-Request-Id attribute is missing.");
        }

        return requestId;
    }
}