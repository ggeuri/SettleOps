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

export function getAdminSettlements(params = {}) {
  return requestJson(`/api/admin/settlements${buildQueryString(params)}`, {
    method: "GET",
  });
}