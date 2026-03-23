import { requestJson } from "./http.js";

function buildQueryString(params) {
  const searchParams = new URLSearchParams();

  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === "") {
      return;
    }
    searchParams.set(key, String(value));
  });

  const query = searchParams.toString();
  return query ? `?${query}` : "";
}

export function getSettlementBatchHistory(params) {
  return requestJson(
    `/api/admin/settlement-batches/history${buildQueryString(params)}`,
    {
      method: "GET",
    }
  );
}

export function runSettlementBatch(baseDate) {
  return requestJson(
    `/api/admin/settlement-batches/run${buildQueryString({ baseDate })}`,
    {
      method: "POST",
    }
  );
}