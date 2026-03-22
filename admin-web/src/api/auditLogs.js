// src/api/auditLogs.js
import { requestJson } from "./http.js";

/**
 * A1 Trace / Audit 조회 API
 *
 * GET /api/admin/audit-logs
 *
 * 지원 파라미터:
 * - requestId
 * - merchantId
 * - entityType
 * - from
 * - to
 * - includeNoOp
 * - page
 * - size
 *
 * 규칙:
 * - requestId / merchantId 중 1개는 필요
 * - 둘 다 있을 때 우선순위는 서버가 판단
 * - merchantId만 있을 때 최근 7일 기본도 서버가 판단
 * - no-op 포함 여부도 서버가 판단
 */
export async function fetchAuditLogs(params = {}) {
  const {
    requestId,
    merchantId,
    entityType,
    from,
    to,
    includeNoOp,
    page,
    size,
  } = params;

  const searchParams = new URLSearchParams();

  if (requestId) searchParams.set("requestId", requestId);
  if (merchantId) searchParams.set("merchantId", merchantId);
  if (entityType) searchParams.set("entityType", entityType);
  if (from) searchParams.set("from", from);
  if (to) searchParams.set("to", to);

  if (includeNoOp === true) {
    searchParams.set("includeNoOp", "true");
  }

  if (page !== undefined && page !== null) {
    searchParams.set("page", String(page));
  }

  if (size !== undefined && size !== null) {
    searchParams.set("size", String(size));
  }

  const query = searchParams.toString();
  const url = query
    ? `/api/admin/audit-logs?${query}`
    : "/api/admin/audit-logs";

  return requestJson(url, {
    method: "GET",
  });
}