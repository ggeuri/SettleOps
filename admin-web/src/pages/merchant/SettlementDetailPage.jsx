import { useMemo } from "react";
import { useNavigate, useParams } from "react-router-dom";
import PageLayout from "../../components/layout/PageLayout.jsx";
import SectionCard from "../../components/layout/SectionCard.jsx";
import SettlementSummarySection from "../../components/settlement/SettlementSummarySection.jsx";
import SettlementAmountSection from "../../components/settlement/SettlementAmountSection.jsx";
import SettlementLineTable from "../../components/settlement/SettlementLineTable.jsx";

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
      <SettlementSummarySection
        settlementId={detail.settlementId}
        merchantId={detail.merchantId}
        status={detail.status}
        baseDate={detail.baseDate}
      />

      <SettlementAmountSection
        gross={detail.gross}
        fee={detail.fee}
        vat={detail.vat}
        net={detail.net}
        createdAt={detail.createdAt}
        paidAt={detail.paidAt}
        approvedRefundExists={detail.approvedRefundExists}
        refundAdjustmentAmount={detail.refundAdjustmentAmount}
      />

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

      <SettlementLineTable
        lines={detail.lines}
        toolbarRight={
          <button
            type="button"
            className="btn btn--secondary"
            onClick={() => navigate("/merchant/settlements")}
          >
            목록으로
          </button>
        }
      />
    </PageLayout>
  );
}