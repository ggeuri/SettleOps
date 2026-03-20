// /admin-web/src/api/paymentApi.js

import { requestJson } from "./http.js";

// TODO: C2(B 담당)에서 구현
export function payOrder(orderId, idempotencyKey) {
    return requestJson(`/api/consumer/orders/${orderId}/pay`, {
        method: "POST",
        headers: {
            "X-Idempotency-Key": idempotencyKey,
        },
    });
}