import { requestJson } from "./http.js";

export function createConsumerOrder({ merchantId, buyerId, itemName, amount }) {
  return requestJson("/api/consumer/orders", {
    method: "POST",
    body: JSON.stringify({
      merchantId,
      buyerId,
      itemName,
      amount,
    }),
  });
}

// C2 상세
export function getConsumerOrderDetail(orderId) {
  return requestJson(`/api/consumer/orders/${orderId}`, {
    method: "GET",
  });
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

  return requestJson(url, {
    method: "GET",
  });
}