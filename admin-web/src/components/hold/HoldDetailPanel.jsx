import CopyableId from "../display/CopyableId.jsx";
import { formatDateTimeWithSeconds } from "../../utils/format.js";

function InfoItem({ label, children }) {
  return (
    <div>
      <strong>{label}</strong> {children}
    </div>
  );
}

export default function HoldDetailPanel({
  item,
  actionLoading = false,
  actionErrorMessage = "",
  actionSuccessMessage = "",
  onApprove,
  onRelease,
  onMoveSettlementDetail,
}) {
  if (!item) {
    return (
      <div
        style={{
          border: "1px solid #e5e7eb",
          borderRadius: "12px",
          padding: "16px",
          background: "#fff",
        }}
      >
        <strong>Hold 상세</strong>
        <div style={{ marginTop: "8px", color: "#6b7280" }}>
          좌측에서 Hold row를 선택해 주세요.
        </div>
      </div>
    );
  }

  return (
    <div
      style={{
        border: "1px solid #e5e7eb",
        borderRadius: "12px",
        padding: "16px",
        background: "#fff",
      }}
    >
      <div style={{ marginBottom: "12px" }}>
        <strong>Hold 상세</strong>
      </div>

      <div
        style={{
          display: "grid",
          gap: "8px",
        }}
      >
        <InfoItem label="holdId">
          <CopyableId value={item?.holdId} />
        </InfoItem>

        <InfoItem label="settlementId">
          <CopyableId value={item?.settlementId} />
        </InfoItem>

        <InfoItem label="merchantId">
          <CopyableId value={item?.merchantId} />
        </InfoItem>

        <InfoItem label="status">{item?.status || "-"}</InfoItem>

        <InfoItem label="reasonCode">{item?.reasonCode || "-"}</InfoItem>

        <InfoItem label="createdAt">
          {formatDateTimeWithSeconds(item?.createdAt)}
        </InfoItem>

        <InfoItem label="requestedComment">
          {item?.requestedComment || "-"}
        </InfoItem>
      </div>

      <div
        style={{
          marginTop: "16px",
          padding: "12px",
          border: "1px solid #f1f5f9",
          borderRadius: "8px",
          background: "#fff7ed",
          color: "#9a3412",
          fontSize: "14px",
          lineHeight: 1.5,
        }}
      >
        <strong>운영 규칙</strong>
        <div style={{ marginTop: "6px" }}>
          A5 목록은 hold 단위로 보여주고, payment 연결 정보는 A4 정산 상세에서
          조회합니다.
        </div>
      </div>

      {actionSuccessMessage && (
        <div style={{ marginTop: "16px", color: "#166534" }}>
          {actionSuccessMessage}
        </div>
      )}

      {actionErrorMessage && (
        <div style={{ marginTop: "16px", color: "#b91c1c" }}>
          {actionErrorMessage}
        </div>
      )}

      <div
        className="action-panel"
        style={{ marginTop: "20px", display: "flex", gap: "8px", flexWrap: "wrap" }}
      >
        <button
          type="button"
          className="btn btn--primary"
          onClick={onApprove}
          disabled={actionLoading}
        >
          {actionLoading ? "처리 중..." : "Approve"}
        </button>

        <button
          type="button"
          className="btn btn--secondary"
          onClick={onRelease}
          disabled={actionLoading}
        >
          {actionLoading ? "처리 중..." : "Release"}
        </button>

        <button
          type="button"
          className="btn btn--secondary"
          onClick={onMoveSettlementDetail}
          disabled={!item?.settlementId}
        >
          A4 정산 상세로 이동
        </button>
      </div>
    </div>
  );
}