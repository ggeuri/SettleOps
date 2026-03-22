import { requestJson } from "./http.js";

export function createConsumerOrder({ merchantId, itemName, amount }) {
  return requestJson("/api/consumer/orders", {
    method: "POST",
    body: JSON.stringify({
      merchantId,
      itemName,
      amount,
    }),
  });
}

// order detail page load TODO: (C3페이지 에서 C2페이지 호출)
export function getConsumerOrderDetail(orderId) {
  return requestJson(`/api/consumer/orders/${orderId}`);
}

// C3 목록
export function getConsumerOrders({
  status,
  confirmed,
  keyword,
} = {}) {
  const searchParams = new URLSearchParams();

  if (status && status !== "ALL") {
    searchParams.set("status", status);
  }

  if (confirmed && confirmed !== "ALL") {
    searchParams.set("confirmed", confirmed);
  }

  if (keyword?.trim()) {
    searchParams.set("keyword", keyword.trim());
  }

  const queryString = searchParams.toString();
  const url = queryString
    ? `/api/consumer/orders?${queryString}`
    : `/api/consumer/orders`;

  return requestJson(url);
}