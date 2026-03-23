// /admin-web/src/api/merchantPaymentApi.js

import { requestJson } from "./http.js";

export function getMerchantPayments({
  merchantId,
  status,
  from,
  to,
  keyword,
  page,
  size,
} = {}) {
  if (!merchantId) {
    throw new Error("merchantId is required");
  }

  const searchParams = new URLSearchParams();

  if (status && status !== "ALL") {
    searchParams.set("status", status);
  }
  if (from) {
    searchParams.set("from", from);
  }
  if (to) {
    searchParams.set("to", to);
  }
  if (keyword?.trim()) {
    searchParams.set("keyword", keyword.trim());
  }
  if (Number.isInteger(page)) {
    searchParams.set("page", String(page));
  }
  if (Number.isInteger(size)) {
    searchParams.set("size", String(size));
  }

  const queryString = searchParams.toString();
  const url = queryString
    ? `/api/merchants/${merchantId}/payments?${queryString}`
    : `/api/merchants/${merchantId}/payments`;

  return requestJson(url);
}

export function getPaymentDetail(paymentId) {
  return requestJson(`/api/payments/${paymentId}`);
}

export function getRefundContext(paymentId) {
  return requestJson(`/api/payments/${paymentId}/refund-context`);
}