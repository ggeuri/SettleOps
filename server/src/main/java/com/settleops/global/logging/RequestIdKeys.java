package com.settleops.global.logging;

public final class RequestIdKeys {
    private RequestIdKeys() {}

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    // HttpServletRequest attribute key for requestId (Filter → Controller 전달용)
    public static final String ATTR_KEY = "requestId";
}