import { requestJson } from "./http.js";

export function getAdminSettlementDetail(settlementId, options = {}) {
  return requestJson(`/api/admin/settlements/${settlementId}`, {
    method: "GET",
    signal: options.signal,
  });
}

export function getAdminSettlementTraceEntry(settlementId, options = {}) {
  return requestJson(`/api/admin/settlements/${settlementId}/trace-entry`, {
    method: "GET",
    signal: options.signal,
  });
}

export function requestAdminSettlementPaid(settlementId) {
  return requestJson(`/api/admin/settlements/${settlementId}/request-paid`, {
    method: "PATCH",
    body: JSON.stringify({}),
  });
}

export function approveAdminSettlementPaid(settlementId) {
  return requestJson(`/api/admin/settlements/${settlementId}/approve-paid`, {
    method: "PATCH",
    body: JSON.stringify({}),
  });
}