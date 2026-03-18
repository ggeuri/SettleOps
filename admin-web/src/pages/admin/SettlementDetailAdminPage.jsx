import { useMemo } from "react";
import { useNavigate, useParams } from "react-router-dom";
import PageLayout from "../../components/layout/PageLayout.jsx";
import SectionCard from "../../components/layout/SectionCard.jsx";
import StatusBadge from "../../components/display/StatusBadge.jsx";
import SettlementSummarySection from "../../components/settlement/SettlementSummarySection.jsx";
import SettlementAmountSection from "../../components/settlement/SettlementAmountSection.jsx";
import SettlementLineTable from "../../components/settlement/SettlementLineTable.jsx";

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
      <SettlementSummarySection
        settlementId={detail.settlementId}
        merchantId={detail.merchantId}
        status={detail.status}
        baseDate={detail.baseDate}
      />

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

      <SettlementAmountSection
        gross={detail.gross}
        fee={detail.fee}
        vat={detail.vat}
        net={detail.net}
        createdAt={detail.createdAt}
        paidRequestedAt={detail.paidRequestedAt}
        paidAt={detail.paidAt}
        requestId={detail.requestId}
        approvedRefundExists={detail.refund.approvedRefundExists}
        refundAdjustmentAmount={detail.refundAdjustmentAmount}
      />

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

      <SettlementLineTable lines={detail.lines} />
    </PageLayout>
  );
}