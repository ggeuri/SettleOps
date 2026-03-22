function formatDateTime(value) {
  if (!value) return "-";

  try {
    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
      return value;
    }

    return new Intl.DateTimeFormat("ko-KR", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
      hour12: false,
    }).format(date);
  } catch {
    return value;
  }
}

function buildApproveDisabled(item, actionLoading) {
  if (!item || actionLoading) return true;
  return false;
}

function buildReleaseDisabled(item, actionLoading) {
  if (!item || actionLoading) return true;
  return false;
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
      <div className="info-list">
        <div>
          <strong>상세 패널</strong> 좌측 Hold row를 선택해 주세요.
        </div>
        <div>
          <strong>표시 기준</strong> A5는 hold 1건 = row 1개입니다.
        </div>
        <div>
          <strong>연결 조회</strong> payment 정보는 A4 정산 상세에서 확인합니다.
        </div>
      </div>
    );
  }

  return (
    <div className="page-section">
      <div className="table-toolbar">
        <div>Hold 상세</div>
        <div className="action-panel">
          <span className="badge badge--default">A5</span>
          <span className="badge badge--primary">hold 기준</span>
        </div>
      </div>

      <div className="kv-list">
        <div className="kv-item">
          <div className="kv-item__label">holdId</div>
          <div className="kv-item__value">
            <span className="display-field copyable-id__text">
              {item?.holdId || "-"}
            </span>
          </div>
        </div>

        <div className="kv-item">
          <div className="kv-item__label">settlementId</div>
          <div className="kv-item__value">
            <span className="display-field copyable-id__text">
              {item?.settlementId || "-"}
            </span>
          </div>
        </div>

        <div className="kv-item">
          <div className="kv-item__label">merchantId</div>
          <div className="kv-item__value">
            <span className="display-field">{item?.merchantId || "-"}</span>
          </div>
        </div>

        <div className="kv-item">
          <div className="kv-item__label">status</div>
          <div className="kv-item__value">
            <span className="display-field">{item?.status || "-"}</span>
          </div>
        </div>

        <div className="kv-item">
          <div className="kv-item__label">reasonCode</div>
          <div className="kv-item__value">
            <span className="display-field">{item?.reasonCode || "-"}</span>
          </div>
        </div>

        <div className="kv-item">
          <div className="kv-item__label">createdAt</div>
          <div className="kv-item__value">
            <span className="display-field">
              {formatDateTime(item?.createdAt)}
            </span>
          </div>
        </div>

        <div className="kv-item">
          <div className="kv-item__label">requestedComment</div>
          <div className="kv-item__value">
            <span className="display-field">{item?.requestedComment || "-"}</span>
          </div>
        </div>
      </div>

      <div className="guard-notice" style={{ marginTop: "16px" }}>
        <div className="guard-notice__title">운영 규칙</div>
        <div className="guard-notice__description">
          A5 목록은 hold 단위로만 보여주고, payment 연결 정보는 A4 정산 상세에서
          조회합니다.
        </div>
      </div>

      {actionErrorMessage && (
        <div className="info-list" style={{ marginTop: "16px" }}>
          <div>
            <strong>오류</strong> {actionErrorMessage}
          </div>
        </div>
      )}

      {actionSuccessMessage && (
        <div className="info-list" style={{ marginTop: "16px" }}>
          <div>
            <strong>완료</strong> {actionSuccessMessage}
          </div>
        </div>
      )}

      <div className="action-panel" style={{ marginTop: "16px" }}>
        <button
          type="button"
          className="btn btn--primary"
          disabled={buildApproveDisabled(item, actionLoading)}
          onClick={() => onApprove?.()}
        >
          {actionLoading ? "처리 중..." : "Approve"}
        </button>

        <button
          type="button"
          className="btn btn--secondary"
          disabled={buildReleaseDisabled(item, actionLoading)}
          onClick={() => onRelease?.()}
        >
          {actionLoading ? "처리 중..." : "Release"}
        </button>

        <button
          type="button"
          className="btn btn--secondary"
          onClick={() => onMoveSettlementDetail?.(item?.settlementId)}
          disabled={!item?.settlementId || actionLoading}
        >
          A4 정산 상세로 이동
        </button>
      </div>
    </div>
  );
}