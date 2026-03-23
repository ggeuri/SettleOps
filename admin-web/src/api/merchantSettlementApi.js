import { requestJson } from "./http.js";

function buildQueryString(params) {
  const searchParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {
    if (value === undefined || value === null || value === "") {
      return;
    }
    searchParams.set(key, String(value));
  });

  const query = searchParams.toString();
  return query ? `?${query}` : "";
}

export function getMerchantSettlements(merchantId, params = {}) {
  return requestJson(
    `/api/merchants/${encodeURIComponent(merchantId)}/settlements${buildQueryString(params)}`,
    {
      method: "GET",
    }
  );
}

export function getMerchantSettlementDetail(
  merchantId,
  settlementId,
  options = {}
) {
  return requestJson(
    `/api/merchants/${encodeURIComponent(
      merchantId
    )}/settlements/${encodeURIComponent(settlementId)}`,
    {
      method: "GET",
      signal: options.signal,
    }
  );
}