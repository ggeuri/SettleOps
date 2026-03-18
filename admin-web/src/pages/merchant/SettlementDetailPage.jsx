import { useMemo } from "react";
import { useNavigate, useParams } from "react-router-dom";
import PageLayout from "../../components/layout/PageLayout.jsx";
import SectionCard from "../../components/layout/SectionCard.jsx";
import StatusBadge from "../../components/display/StatusBadge.jsx";

const MOCK_SETTLEMENT_DETAIL = {
  settlementId: "SET-20260318-0001",
  merchantId: "MRC_1001",
  status: "READY",
  baseDate: "2026-03-18",
  gross: 125000,
  fee: 3750,
  vat: 375,
  refundAdjustmentAmount: 12000,
  net: 108875,
  createdAt: "2026-03-18 10:30:00",
  paidAt: null,
  refundAdjustmentPending: true,
  approvedRefundExists: true,
  lines: [
    {
      lineId: "LINE-001",
      type: "PAYMENT",
      paymentId: "PAY-20260318-0001",
      amount: 120875,
    },
    {
      lineId: "LINE-002",
      type: "REFUND",
      paymentId: "PAY-20260310-0044",
      amount: 12000,
    },
  ],
};

function formatAmount(value) {
  if (typeof value !== "number") return "-";
  return `${value.toLocaleString("ko-KR")}원`;
}

function shortId(value) {
  if (!value) return "-";
  if (value.length <= 16) return value;
  return `${value.slice(0, 8)}...${value.slice(-4)}`;
}

export default function SettlementDetailPage() {
  const navigate = useNavigate();
  const { settlementId } = useParams();

  const detail = useMemo(() => {
    return {
      ...MOCK_SETTLEMENT_DETAIL,
      settlementId: settlementId || MOCK_SETTLEMENT_DETAIL.settlementId,
    };
  }, [settlementId]);

  return (
    <PageLayout
      title="정산 상세"
      description="판매자 기준 정산 상세를 조회합니다. 정산 요약과 라인 내역을 확인할 수 있습니다."
    >
      <SectionCard title="기본 정보">
        <div className="summary-card-grid">
          <div className="card">
            <div className="card__body">
              <div className="summary-card__label">settlementId</div>
              <div className="summary-card__value" style={{ fontSize: "16px" }}>
                <div className="copyable-id">
                  <span className="copyable-id__text copyable-id__text--short">
                    {detail.settlementId}
                  </span>
                </div>
              </div>
            </div>
          </div>

          <div className="card">
            <div className="card__body">
              <div className="summary-card__label">merchantId</div>
              <div className="summary-card__value" style={{ fontSize: "16px" }}>
                {detail.merchantId}
              </div>
            </div>
          </div>

          <div className="card">
            <div className="card__body">
              <div className="summary-card__label">status</div>
              <div className="summary-card__value" style={{ fontSize: "16px" }}>
                <StatusBadge status={detail.status} />
              </div>
            </div>
          </div>

          <div className="card">
            <div className="card__body">
              <div className="summary-card__label">baseDate</div>
              <div className="summary-card__value" style={{ fontSize: "16px" }}>
                {detail.baseDate}
              </div>
            </div>
          </div>
        </div>
      </SectionCard>

      <SectionCard title="금액 요약">
        <div className="summary-card-grid">
          <div className="card">
            <div className="card__body">
              <div className="summary-card__label">gross</div>
              <div className="summary-card__value">
                {formatAmount(detail.gross)}
              </div>
            </div>
          </div>

          <div className="card">
            <div className="card__body">
              <div className="summary-card__label">fee</div>
              <div className="summary-card__value">
                -{formatAmount(detail.fee)}
              </div>
            </div>
          </div>

          <div className="card">
            <div className="card__body">
              <div className="summary-card__label">vat</div>
              <div className="summary-card__value">
                -{formatAmount(detail.vat)}
              </div>
            </div>
          </div>

          <div className="card">
            <div className="card__body">
              <div className="summary-card__label">net</div>
              <div className="summary-card__value">
                {formatAmount(detail.net)}
              </div>
            </div>
          </div>
        </div>

        <div className="table-wrap" style={{ marginTop: "16px" }}>
          <table className="data-table">
            <tbody>
              <tr>
                <th>createdAt</th>
                <td>{detail.createdAt}</td>
              </tr>
              <tr>
                <th>paidAt</th>
                <td>{detail.paidAt || "-"}</td>
              </tr>
              <tr>
                <th>approvedRefundExists</th>
                <td>{detail.approvedRefundExists ? "Y" : "N"}</td>
              </tr>
              <tr>
                <th>refundAdjustmentAmount</th>
                <td>{formatAmount(detail.refundAdjustmentAmount)}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </SectionCard>

      {detail.refundAdjustmentPending ? (
        <SectionCard title="안내">
          <div className="guard-notice">
            <strong>차감정산 반영 대기</strong>
            <p style={{ marginTop: "8px" }}>
              승인된 환불이 존재하며, 다음 배치에서 REFUND 라인으로 반영될 수 있습니다.
            </p>
          </div>
        </SectionCard>
      ) : null}

      <SectionCard title="정산 라인">
        <div className="table-toolbar">
          <div>settlement_line 목록</div>
          <div className="action-panel">
            <button
              type="button"
              className="btn btn--secondary"
              onClick={() => navigate("/merchant/settlements")}
            >
              목록으로
            </button>
          </div>
        </div>

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
              {detail.lines.map((line) => (
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
              ))}
            </tbody>
          </table>
        </div>
      </SectionCard>
    </PageLayout>
  );
}