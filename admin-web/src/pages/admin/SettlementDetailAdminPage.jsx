import { useMemo } from "react";
import { useNavigate, useParams } from "react-router-dom";
import PageLayout from "../../components/layout/PageLayout.jsx";
import SectionCard from "../../components/layout/SectionCard.jsx";
import StatusBadge from "../../components/display/StatusBadge.jsx";

const MOCK_SETTLEMENT_DETAIL = {
  settlementId: "SET-20260318-0001",
  merchantId: "MRC_1001",
  status: "HOLD_ACTIVE",
  baseDate: "2026-03-18",
  gross: 125000,
  fee: 3750,
  vat: 375,
  refundAdjustmentAmount: 12000,
  net: 108875,
  createdAt: "2026-03-18 10:30:00",
  paidRequestedAt: null,
  paidAt: null,
  requestId: "REQ-20260318-ADMIN-0001",

  hold: {
    exists: true,
    holdId: "HOLD-20260318-0001",
    status: "HOLD_ACTIVE",
    reasonCode: "RISK_REVIEW",
    memo: "고위험 거래 검토 필요",
    approvedBy: "admin02",
    approvedAt: "2026-03-18 13:15:00",
  },

  refund: {
    approvedRefundExists: true,
    refundAdjustmentPending: true,
    refundCount: 1,
    latestRefundId: "REF-20260318-0001",
  },

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

export default function SettlementDetailAdminPage() {
  const navigate = useNavigate();
  const { settlementId } = useParams();

  const detail = useMemo(() => {
    return {
      ...MOCK_SETTLEMENT_DETAIL,
      settlementId: settlementId || MOCK_SETTLEMENT_DETAIL.settlementId,
    };
  }, [settlementId]);

  const isHoldActive = detail.status === "HOLD_ACTIVE";
  const isRefundAdjustmentPending = detail.refund.refundAdjustmentPending;

  return (
    <PageLayout
      title="정산 상세"
      description="운영 허브입니다. settlementId를 앵커로 정산, hold, refund, trace 동선을 연결합니다."
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

      <SectionCard title="운영 가드레일">
        {isHoldActive ? (
          <div className="guard-notice" style={{ marginBottom: "12px" }}>
            <strong>지급 요청 차단</strong>
            <p style={{ marginTop: "8px" }}>
              Hold가 ACTIVE라 지급요청 불가. Hold 큐(A5)에서 Release 후 다시 시도하세요.
            </p>
          </div>
        ) : null}

        {isRefundAdjustmentPending ? (
          <div className="guard-notice">
            <strong>차감정산 반영 대기</strong>
            <p style={{ marginTop: "8px" }}>
              승인된 환불이 있어 차감정산 반영 전입니다. 다음 배치 실행 후 다시 시도하세요.
            </p>
          </div>
        ) : null}
      </SectionCard>

      <SectionCard title="금액 요약">
        <div className="summary-card-grid">
          <div className="card">
            <div className="card__body">
              <div className="summary-card__label">gross</div>
              <div className="summary-card__value">{formatAmount(detail.gross)}</div>
            </div>
          </div>

          <div className="card">
            <div className="card__body">
              <div className="summary-card__label">fee</div>
              <div className="summary-card__value">-{formatAmount(detail.fee)}</div>
            </div>
          </div>

          <div className="card">
            <div className="card__body">
              <div className="summary-card__label">vat</div>
              <div className="summary-card__value">-{formatAmount(detail.vat)}</div>
            </div>
          </div>

          <div className="card">
            <div className="card__body">
              <div className="summary-card__label">net</div>
              <div className="summary-card__value">{formatAmount(detail.net)}</div>
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
                <th>paidRequestedAt</th>
                <td>{detail.paidRequestedAt || "-"}</td>
              </tr>
              <tr>
                <th>paidAt</th>
                <td>{detail.paidAt || "-"}</td>
              </tr>
              <tr>
                <th>requestId</th>
                <td>
                  <div className="copyable-id">
                    <span className="copyable-id__text copyable-id__text--short">
                      {detail.requestId}
                    </span>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </SectionCard>

      <SectionCard title="운영 액션">
        <div className="button-row action-panel">
          <button type="button" className="btn btn--secondary">
            Hold 생성
          </button>

          <button
            type="button"
            className="btn btn--primary"
            disabled={isHoldActive || isRefundAdjustmentPending}
          >
            request-paid
          </button>

          <button
            type="button"
            className="btn btn--secondary"
            onClick={() => navigate(`/admin/audit?requestId=${detail.requestId}`)}
          >
            Trace로 보기
          </button>
        </div>
      </SectionCard>

      <SectionCard title="관련 화면 이동">
        <div className="button-row action-panel">
          <button
            type="button"
            className="btn btn--secondary"
            onClick={() => navigate("/admin/settlements")}
          >
            정산 관리(A3)
          </button>
          <button
            type="button"
            className="btn btn--secondary"
            onClick={() => navigate("/admin/settlement-batches")}
          >
            Batch 콘솔(A2)
          </button>
          <button
            type="button"
            className="btn btn--secondary"
            onClick={() => navigate("/admin/audit")}
          >
            Trace(A1)
          </button>
          <button type="button" className="btn btn--secondary">
            Hold 큐(A5)
          </button>
          <button type="button" className="btn btn--secondary">
            Refund 큐(A6)
          </button>
        </div>
      </SectionCard>

      <SectionCard title="hold 요약">
        {detail.hold.exists ? (
          <div className="table-wrap">
            <table className="data-table">
              <tbody>
                <tr>
                  <th>holdId</th>
                  <td>{detail.hold.holdId}</td>
                </tr>
                <tr>
                  <th>status</th>
                  <td>
                    <StatusBadge status={detail.hold.status} />
                  </td>
                </tr>
                <tr>
                  <th>reasonCode</th>
                  <td>{detail.hold.reasonCode}</td>
                </tr>
                <tr>
                  <th>memo</th>
                  <td>{detail.hold.memo}</td>
                </tr>
                <tr>
                  <th>approvedBy</th>
                  <td>{detail.hold.approvedBy}</td>
                </tr>
                <tr>
                  <th>approvedAt</th>
                  <td>{detail.hold.approvedAt}</td>
                </tr>
              </tbody>
            </table>
          </div>
        ) : (
          <div>활성 hold가 없습니다.</div>
        )}
      </SectionCard>

      <SectionCard title="refund 요약">
        <div className="table-wrap">
          <table className="data-table">
            <tbody>
              <tr>
                <th>approvedRefundExists</th>
                <td>{detail.refund.approvedRefundExists ? "Y" : "N"}</td>
              </tr>
              <tr>
                <th>refundAdjustmentPending</th>
                <td>
                  <StatusBadge
                    status={
                      detail.refund.refundAdjustmentPending
                        ? "REFUND_ADJUSTMENT_PENDING"
                        : "READY"
                    }
                  />
                </td>
              </tr>
              <tr>
                <th>refundCount</th>
                <td>{detail.refund.refundCount}</td>
              </tr>
              <tr>
                <th>latestRefundId</th>
                <td>{detail.refund.latestRefundId}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </SectionCard>

      <SectionCard title="정산 라인">
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