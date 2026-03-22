// /admin-web/src/api/paymentApi.js
import { requestJson } from "./http.js";

// consumer - pay (결제 승인)
export function payOrder(orderId, idempotencyKey) {
  return requestJson(`/api/consumer/orders/${orderId}/pay`, {
    method: "POST",
    headers: {
      "X-Idempotency-Key": idempotencyKey,
    },
  });
}

// consumer - confirm (구매확정)
export function confirmPayment(paymentId) {
  return requestJson(`/api/consumer/payments/${paymentId}/confirm`, {
    method: "POST",
  });
}