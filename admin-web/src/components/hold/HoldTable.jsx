import Pagination from "../table/Pagination.jsx";
import CopyableId from "../display/CopyableId.jsx";
import { formatDateTimeWithSeconds } from "../../utils/format.js";

function buildRowKey(item, index) {
  return `${item?.holdId ?? "hold"}-${index}`;
}

export default function HoldTable({
  loading = false,
  items = [],
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
        <div>A5 Hold 큐 목록</div>
        <div className="action-panel" style={{ gap: "12px" }}>
          <span>표시 {visibleCount}건</span>
          <span>서버 총 {totalElements}건</span>
        </div>
      </div>

      <div className="table-wrap">
        <table className="data-table">
          <thead>
            <tr>
              <th>createdAt</th>
              <th>holdId</th>
              <th>settlementId</th>
              <th>merchantId</th>
              <th>status</th>
              <th>reasonCode</th>
              <th>requestedComment</th>
            </tr>
          </thead>

          <tbody>
            {loading && (
              <tr>
                <td colSpan={7}>조회 중입니다.</td>
              </tr>
            )}

            {isEmpty && (
              <tr>
                <td colSpan={7}>조회 결과가 없습니다.</td>
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
                    <td>{formatDateTimeWithSeconds(item?.createdAt)}</td>

                    <td
                      onClick={(event) => event.stopPropagation()}
                      style={{ verticalAlign: "middle" }}
                    >
                      <CopyableId value={item?.holdId} short />
                    </td>

                    <td
                      onClick={(event) => event.stopPropagation()}
                      style={{ verticalAlign: "middle" }}
                    >
                      <CopyableId value={item?.settlementId} short />
                    </td>

                    <td
                      onClick={(event) => event.stopPropagation()}
                      style={{ verticalAlign: "middle" }}
                    >
                      <CopyableId value={item?.merchantId} short />
                    </td>

                    <td>{item?.status || "-"}</td>
                    <td>{item?.reasonCode || "-"}</td>
                    <td>{item?.requestedComment || "-"}</td>
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