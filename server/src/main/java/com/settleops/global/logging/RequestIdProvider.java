package com.settleops.global.logging;

import org.slf4j.MDC;

public final class RequestIdProvider {

    private RequestIdProvider() {}

    public static String current() {
        String requestId = MDC.get(RequestIdKeys.MDC_KEY);

        if (requestId == null || requestId.isBlank()) {
            throw new IllegalStateException("request_id is missing in MDC");
        }

        return requestId;
    }
}