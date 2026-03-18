import SectionCard from "../layout/SectionCard.jsx";
import StatusBadge from "../display/StatusBadge.jsx";

function formatAmount(value) {
  if (typeof value !== "number") return "-";
  return `${value.toLocaleString("ko-KR")}원`;
}

function shortId(value) {
  if (!value) return "-";
  if (value.length <= 16) return value;
  return `${value.slice(0, 8)}...${value.slice(-4)}`;
}

export default function SettlementLineTable({
  lines = [],
  title = "정산 라인",
  toolbarRight = null,
}) {
  return (
    <SectionCard title={title}>
      {toolbarRight ? (
        <div className="table-toolbar">
          <div>settlement_line 목록</div>
          <div className="action-panel">{toolbarRight}</div>
        </div>
      ) : null}

      <div className="table-wrap">
        <table className="data-table">
          <thead>
            <tr>
              <th>lineId</th>
              <th>type</th>
              <th>paymentId</th>
              <th>amount</th>
            </tr>
          </thead>
          <tbody>
            {lines.length === 0 ? (
              <tr>
                <td colSpan={4}>정산 라인이 없습니다.</td>
              </tr>
            ) : (
              lines.map((line) => (
                <tr key={line.lineId}>
                  <td>{shortId(line.lineId)}</td>
                  <td>
                    <StatusBadge status={line.type} />
                  </td>
                  <td>
                    <div className="copyable-id">
                      <span className="copyable-id__text copyable-id__text--short">
                        {line.paymentId}
                      </span>
                    </div>
                  </td>
                  <td>
                    <span
                      className={
                        line.type === "REFUND"
                          ? "amount-text amount-text--negative"
                          : "amount-text amount-text--positive"
                      }
                    >
                      {line.type === "REFUND" ? "-" : "+"}
                      {formatAmount(line.amount)}
                    </span>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </SectionCard>
  );
}