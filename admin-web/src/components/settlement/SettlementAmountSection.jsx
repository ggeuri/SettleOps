import SectionCard from "../layout/SectionCard.jsx";

function formatAmount(value) {
  if (typeof value !== "number") return "-";
  return `${value.toLocaleString("ko-KR")}원`;
}

export default function SettlementAmountSection({
  gross,
  fee,
  vat,
  net,
  createdAt,
  paidRequestedAt,
  paidAt,
  requestId,
  approvedRefundExists,
  refundAdjustmentAmount,
}) {
  return (
    <SectionCard title="금액 요약">
      <div className="summary-card-grid">
        <div className="card">
          <div className="card__body">
            <div className="summary-card__label">gross</div>
            <div className="summary-card__value">{formatAmount(gross)}</div>
          </div>
        </div>

        <div className="card">
          <div className="card__body">
            <div className="summary-card__label">fee</div>
            <div className="summary-card__value">-{formatAmount(fee)}</div>
          </div>
        </div>

        <div className="card">
          <div className="card__body">
            <div className="summary-card__label">vat</div>
            <div className="summary-card__value">-{formatAmount(vat)}</div>
          </div>
        </div>

        <div className="card">
          <div className="card__body">
            <div className="summary-card__label">net</div>
            <div className="summary-card__value">{formatAmount(net)}</div>
          </div>
        </div>
      </div>

      <div className="table-wrap" style={{ marginTop: "16px" }}>
        <table className="data-table">
          <tbody>
            <tr>
              <th>createdAt</th>
              <td>{createdAt || "-"}</td>
            </tr>

            {paidRequestedAt !== undefined ? (
              <tr>
                <th>paidRequestedAt</th>
                <td>{paidRequestedAt || "-"}</td>
              </tr>
            ) : null}

            <tr>
              <th>paidAt</th>
              <td>{paidAt || "-"}</td>
            </tr>

            {requestId !== undefined ? (
              <tr>
                <th>requestId</th>
                <td>
                  <div className="copyable-id">
                    <span className="copyable-id__text copyable-id__text--short">
                      {requestId || "-"}
                    </span>
                  </div>
                </td>
              </tr>
            ) : null}

            {approvedRefundExists !== undefined ? (
              <tr>
                <th>approvedRefundExists</th>
                <td>{approvedRefundExists ? "Y" : "N"}</td>
              </tr>
            ) : null}

            {refundAdjustmentAmount !== undefined ? (
              <tr>
                <th>refundAdjustmentAmount</th>
                <td>{formatAmount(refundAdjustmentAmount)}</td>
              </tr>
            ) : null}
          </tbody>
        </table>
      </div>
    </SectionCard>
  );
}