// src/api/auditEvents.js
import { requestJson } from "./http.js";

/**
 * A1 Trace row expand용 event 조회 API
 *
 * GET /api/admin/audit-events?requestId=...
 *
 * 규칙:
 * - requestId 필수
 * - 동일 requestId에 연결된 payment / hold / refund event 반환
 */
export async function fetchAuditEvents(requestId) {
  const searchParams = new URLSearchParams();

  if (requestId) {
    searchParams.set("requestId", requestId);
  }

  const query = searchParams.toString();
  const url = query
    ? `/api/admin/audit-events?${query}`
    : "/api/admin/audit-events";

  return requestJson(url, {
    method: "GET",
  });
}