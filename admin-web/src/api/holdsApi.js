// src/api/holdsApi.js
import { requestJson } from "./http.js";

/**
 * A5 Hold Queue 조회/운영 API
 *
 * GET /api/admin/holds
 * - status
 * - settlementId
 * - merchantId
 * - page
 * - size
 *
 * POST /api/admin/holds
 * PATCH /api/admin/holds/{holdId}/approve
 * PATCH /api/admin/holds/{holdId}/release
 */

export async function fetchHolds(params = {}) {
  const { status, settlementId, merchantId, page, size } = params;

  const searchParams = new URLSearchParams();

  if (status) searchParams.set("status", status);
  if (settlementId) searchParams.set("settlementId", settlementId);
  if (merchantId) searchParams.set("merchantId", merchantId);
  if (page !== undefined && page !== null) searchParams.set("page", String(page));
  if (size !== undefined && size !== null) searchParams.set("size", String(size));

  const query = searchParams.toString();
  const url = query ? `/api/admin/holds?${query}` : "/api/admin/holds";

  return requestJson(url, {
    method: "GET",
  });
}

export async function approveHold(holdId, comment = "") {
  return requestJson(`/api/admin/holds/${encodeURIComponent(holdId)}/approve`, {
    method: "PATCH",
    body: JSON.stringify({
      comment,
    }),
  });
}

export async function releaseHold(holdId, comment = "") {
  return requestJson(`/api/admin/holds/${encodeURIComponent(holdId)}/release`, {
    method: "PATCH",
    body: JSON.stringify({
      comment,
    }),
  });
}