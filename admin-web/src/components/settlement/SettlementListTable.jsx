import SectionCard from "../layout/SectionCard.jsx";
import StatusBadge from "../display/StatusBadge.jsx";

function formatAmount(value) {
  return `${value.toLocaleString("ko-KR")}원`;
}

export default function SettlementListTable({
  items = [],
  onRowClick,
  toolbarTitle = "정산 목록",
  toolbarRight = null,
}) {
  return (
    <SectionCard title="목록">
      <div className="table-toolbar">
        <div>{toolbarTitle}</div>
        <div className="action-panel">
          {toolbarRight || (
            <button type="button" className="btn btn--secondary">
              새로고침
            </button>
          )}
        </div>
      </div>

      <div className="table-wrap">
        <table className="data-table">
          <thead>
            <tr>
              <th>settlementId</th>
              <th>merchantId</th>
              <th>status</th>
              <th>gross</th>
              <th>fee</th>
              <th>vat</th>
              <th>net</th>
              <th>baseDate</th>
              <th>createdAt</th>
            </tr>
          </thead>
          <tbody>
            {items.length === 0 ? (
              <tr>
                <td colSpan={9}>조회 결과가 없습니다.</td>
              </tr>
            ) : (
              items.map((item) => (
                <tr
                  key={item.settlementId}
                  onClick={() => onRowClick(item.settlementId)}
                  style={{ cursor: "pointer" }}
                  title={item.settlementId}
                >
                  <td>
                    <div className="copyable-id">
                      <span className="copyable-id__text copyable-id__text--short">
                        {item.settlementId}
                      </span>
                    </div>
                  </td>
                  <td>{item.merchantId}</td>
                  <td>
                    <StatusBadge status={item.status} />
                  </td>
                  <td>
                    <span className="amount-text">{formatAmount(item.gross)}</span>
                  </td>
                  <td>
                    <span className="amount-text amount-text--negative">
                      -{formatAmount(item.fee)}
                    </span>
                  </td>
                  <td>
                    <span className="amount-text amount-text--negative">
                      -{formatAmount(item.vat)}
                    </span>
                  </td>
                  <td>
                    <span className="amount-text">{formatAmount(item.net)}</span>
                  </td>
                  <td>{item.baseDate}</td>
                  <td>{item.createdAt}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <div className="pagination">
        <button type="button" className="btn btn--secondary">
          이전
        </button>
        <button type="button" className="btn btn--primary">
          1
        </button>
        <button type="button" className="btn btn--secondary">
          다음
        </button>
      </div>
    </SectionCard>
  );
}