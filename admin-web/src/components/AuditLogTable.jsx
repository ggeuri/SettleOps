function formatDateTime(value) {
  if (!value) return "-";

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  const yyyy = date.getFullYear();
  const mm = String(date.getMonth() + 1).padStart(2, "0");
  const dd = String(date.getDate()).padStart(2, "0");
  const hh = String(date.getHours()).padStart(2, "0");
  const mi = String(date.getMinutes()).padStart(2, "0");
  const ss = String(date.getSeconds()).padStart(2, "0");

  return `${yyyy}-${mm}-${dd} ${hh}:${mi}:${ss}`;
}

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

  function handlePrevPage() {
    if (page <= 0) return;
    onPageChange?.(page - 1);
  }

  function handleNextPage() {
    if (page >= totalPages - 1) return;
    onPageChange?.(page + 1);
  }

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
                    <td>{formatDateTime(item?.occurredAt)}</td>
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

      <div className="pagination">
        <button
          type="button"
          className="btn btn--secondary"
          onClick={handlePrevPage}
          disabled={loading || page <= 0}
        >
          이전
        </button>

        <button type="button" className="btn btn--primary" disabled>
          {totalPages === 0 ? 0 : page + 1}
        </button>

        <button
          type="button"
          className="btn btn--secondary"
          onClick={handleNextPage}
          disabled={loading || totalPages === 0 || page >= totalPages - 1}
        >
          다음
        </button>
      </div>

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