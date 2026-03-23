import Pagination from "../../components/table/Pagination.jsx";
import { formatDateTimeWithSeconds } from "../../utils/format.js";

function buildActorLabel(item) {
  const actorType = item?.actorType || "-";
  const actorId = item?.actorId || "-";
  return `${actorType} / ${actorId}`;
}

function buildStatusChangeLabel(item) {
  const before = item?.statusBefore;
  const after = item?.statusAfter;

  if (!before && !after) return "-";
  return `${before || "-"} → ${after || "-"}`;
}

function buildRowKey(item, index) {
  return `${item?.auditId ?? "audit"}-${index}`;
}

export default function AuditLogTable({
  loading = false,
  items = [],
  requestId = null,
  page = 0,
  size = 20,
  totalElements = 0,
  totalPages = 0,
  visibleCount = 0,
  selectedRowKey = "",
  onSelectItem,
  onPageChange,
}) {
  const isEmpty = !loading && items.length === 0;
  const startRow = totalElements === 0 ? 0 : page * size + 1;
  const endRow =
    totalElements === 0 ? 0 : Math.min((page + 1) * size, totalElements);

  return (
    <>
      <div className="table-toolbar">
        <div>
          {requestId ? `requestId 기준 조회: ${requestId}` : "Trace / Audit 로그 목록"}
        </div>
        <div className="action-panel" style={{ gap: "12px" }}>
          <span>표시 {visibleCount}건</span>
          <span>서버 총 {totalElements}건</span>
        </div>
      </div>

      <div className="table-wrap">
        <table className="data-table">
          <thead>
            <tr>
              <th>occurredAt</th>
              <th>actor</th>
              <th>action</th>
              <th>entityType</th>
              <th>entityId</th>
              <th>statusChange</th>
            </tr>
          </thead>

          <tbody>
            {loading && (
              <tr>
                <td colSpan={6}>조회 중입니다.</td>
              </tr>
            )}

            {isEmpty && (
              <tr>
                <td colSpan={6}>조회 결과가 없습니다.</td>
              </tr>
            )}

            {!loading &&
              items.map((item, index) => {
                const rowKey = buildRowKey(item, index);
                const isSelected = rowKey === selectedRowKey;

                return (
                  <tr
                    key={rowKey}
                    onClick={() => onSelectItem?.(rowKey)}
                    style={{
                      cursor: "pointer",
                      backgroundColor: isSelected ? "#F8FAFC" : "",
                    }}
                  >
                    <td>{formatDateTimeWithSeconds(item?.occurredAt)}</td>
                    <td>{buildActorLabel(item)}</td>
                    <td>{item?.action || "-"}</td>
                    <td>{item?.entityType || "-"}</td>
                    <td>{item?.entityId || "-"}</td>
                    <td>{buildStatusChangeLabel(item)}</td>
                  </tr>
                );
              })}
          </tbody>
        </table>
      </div>

      <Pagination
        page={page}
        totalPages={totalPages}
        onPageChange={onPageChange}
        disabled={loading}
      />

      <div className="table-toolbar" style={{ marginTop: "12px" }}>
        <div>
          {startRow} - {endRow} / {totalElements}
        </div>
        <div>
          페이지 {totalPages === 0 ? 0 : page + 1} / {totalPages}
        </div>
      </div>
    </>
  );
}