import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";

import PageLayout from "../../components/layout/PageLayout.jsx";
import SectionCard from "../../components/layout/SectionCard.jsx";
import ActionButton from "../../components/layout/ActionButton.jsx";
import StatusBadge from "../../components/display/StatusBadge.jsx";
import CopyableId from "../../components/display/CopyableId.jsx";
import InfoRow from "../../components/display/InfoRow.jsx";
import GuardNotice from "../../components/common/GuardNotice.jsx";
import LoadingBlock from "../../components/feedback/LoadingBlock.jsx";
import ErrorState from "../../components/feedback/ErrorState.jsx";
import RequireLoginNotice from "../../components/feedback/RequireLoginNotice.jsx";

import { getMe } from "../../api/meApi.js";
import {
  getAdminSettlementDetail,
  getAdminSettlementTraceEntry,
  requestAdminSettlementPaid,
} from "../../api/adminSettlementApi.js";

function formatAmount(value) {
  if (value === null || value === undefined || value === "") return "-";
  const num = Number(value);
  if (Number.isNaN(num)) return String(value);
  return `${num.toLocaleString("ko-KR")}원`;
}

function extractRole(payload) {
  return (
    payload?.role ||
    payload?.data?.role ||
    payload?.user?.role ||
    payload?.principal?.role ||
    null
  );
}

function buildAuthErrorMessage(error) {
  const status = error?.status;
  const message =
    error?.body?.message ||
    error?.body?.reason ||
    error?.message;

  if (status === 401) {
    return "인증이 만료되었거나 로그인되지 않았습니다. /auth/dev-login 에서 Admin 세션을 다시 생성해 주세요.";
  }

  if (status === 403) {
    return "Admin 권한이 없어 정산 상세 화면에 접근할 수 없습니다.";
  }

  return (
    message ||
    "현재 세션의 역할을 확인할 수 없습니다. /auth/dev-login 에서 Admin 세션으로 다시 로그인해 주세요."
  );
}

function mapDetailErrorToMessage(error) {
  const status = error?.status;
  const message =
    error?.body?.message ||
    error?.body?.reason ||
    error?.message;

  if (status === 401) {
    return "인증이 만료되었거나 로그인되지 않았습니다. /auth/dev-login 에서 Admin 세션을 다시 생성해 주세요.";
  }

  if (status === 403) {
    return "Admin 권한이 없어 정산 상세를 조회할 수 없습니다.";
  }

  if (status === 404) {
    return "정산 정보를 찾을 수 없습니다.";
  }

  return message || "정산 상세 조회 중 오류가 발생했습니다.";
}

function mapRequestPaidReasonToMessage(reason) {
  switch (reason) {
    case "HOLD_ACTIVE":
      return "Hold가 ACTIVE라 지급요청 불가입니다. Hold 큐(A5)에서 Release 후 다시 시도해야 합니다.";
    case "REFUND_ADJUSTMENT_PENDING":
      return "승인된 환불이 있어 차감정산 반영 전입니다. 다음 배치 실행 후 다시 시도해야 합니다.";
    case "SETTLEMENT_NOT_READY":
      return "READY 상태에서만 지급 요청이 가능합니다.";
    case "BATCH_FAILED":
      return "FAIL 배치 산출 건은 지급 요청할 수 없습니다. 배치 이력(A2)에서 원인을 먼저 확인해 주세요.";
    default:
      return "현재 상태에서는 지급 요청이 불가능합니다.";
  }
}

function mapTraceEntryErrorToMessage(error) {
  const status = error?.status;
  const message =
    error?.body?.message ||
    error?.body?.reason ||
    error?.message;

  if (status === 401) {
    return "인증이 만료되었거나 로그인되지 않았습니다. /auth/dev-login 에서 Admin 세션을 다시 생성해 주세요.";
  }

  if (status === 403) {
    return "Admin 권한이 없어 Trace로 이동할 수 없습니다.";
  }

  if (status === 404) {
    return "이 정산 건에 대한 Trace 진입 가능한 request_id가 아직 없습니다.";
  }

  return message || "Trace 진입 정보를 조회하는 중 오류가 발생했습니다.";
}

function isRefundAdjustmentPending(refund) {
  return refund?.refundAdjustmentPending === true;
}

function hasApprovedRefund(refund) {
  return (
    refund?.hasApprovedRefund === true ||
    refund?.approvedRefundExists === true
  );
}

function buildHoldQueueLink(settlementId) {
  return settlementId
    ? `/admin/holds?settlementId=${encodeURIComponent(settlementId)}`
    : "/admin/holds";
}

function buildHoldCreateLink(settlementId) {
  return settlementId
    ? `/admin/holds?settlementId=${encodeURIComponent(settlementId)}&mode=create`
    : "/admin/holds";
}

function buildRefundQueueLink(settlementId) {
  return settlementId
    ? `/admin/refunds?settlementId=${encodeURIComponent(settlementId)}`
    : "/admin/refunds";
}

function CopyableValue({ value }) {
  if (!value || value === "-") {
    return <span>-</span>;
  }

  return <CopyableId value={value} short />;
}

export default function SettlementDetailAdminPage() {
  const { settlementId } = useParams();
  const navigate = useNavigate();

  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");

  const [requestPaidLoading, setRequestPaidLoading] = useState(false);
  const [traceEntryLoading, setTraceEntryLoading] = useState(true);

  const [actionMessage, setActionMessage] = useState("");
  const [actionError, setActionError] = useState("");
  const [traceEntryError, setTraceEntryError] = useState("");
  const [traceRequestId, setTraceRequestId] = useState("");

  const [authChecking, setAuthChecking] = useState(true);
  const [isAllowed, setIsAllowed] = useState(false);
  const [authErrorMessage, setAuthErrorMessage] = useState("");

  const fetchDetail = useCallback(
    async (signal) => {
      try {
        setLoading(true);
        setLoadError("");

        const data = await getAdminSettlementDetail(settlementId, { signal });
        setDetail(data || null);
      } catch (error) {
        if (error?.name === "AbortError" || error?.code === "ERR_CANCELED") {
          return;
        }
        setDetail(null);
        setLoadError(mapDetailErrorToMessage(error));
      } finally {
        setLoading(false);
      }
    },
    [settlementId]
  );

  const fetchTraceEntry = useCallback(
    async (signal) => {
      try {
        setTraceEntryLoading(true);
        setTraceEntryError("");
        setTraceRequestId("");

        const data = await getAdminSettlementTraceEntry(settlementId, { signal });

        const requestId =
          data?.traceRequestId ||
          data?.requestId ||
          data?.resolvedRequestId ||
          "";

        if (!requestId) {
          setTraceEntryError(
            "이 정산 건에 대한 Trace 진입 가능한 request_id가 아직 없습니다."
          );
          setTraceRequestId("");
          return;
        }

        setTraceRequestId(requestId);
      } catch (error) {
        if (error?.name === "AbortError" || error?.code === "ERR_CANCELED") {
          return;
        }
        setTraceRequestId("");
        setTraceEntryError(mapTraceEntryErrorToMessage(error));
      } finally {
        setTraceEntryLoading(false);
      }
    },
    [settlementId]
  );

  const reloadPageData = useCallback(
    async (signal) => {
      await Promise.all([fetchDetail(signal), fetchTraceEntry(signal)]);
    },
    [fetchDetail, fetchTraceEntry]
  );

  useEffect(() => {
    let mounted = true;

    async function checkAdminRole() {
      try {
        setAuthChecking(true);
        setAuthErrorMessage("");
        setIsAllowed(false);

        const me = await getMe();
        const role = String(extractRole(me) || "").toUpperCase();

        if (!mounted) return;

        if (role === "ADMIN") {
          setIsAllowed(true);
          return;
        }

        setIsAllowed(false);
        setAuthErrorMessage(
          "Admin 전용 정산 상세 화면입니다. /auth/dev-login 에서 Admin 세션으로 다시 로그인해 주세요."
        );
      } catch (error) {
        if (!mounted) return;
        setIsAllowed(false);
        setAuthErrorMessage(buildAuthErrorMessage(error));
      } finally {
        if (mounted) {
          setAuthChecking(false);
        }
      }
    }

    checkAdminRole();

    return () => {
      mounted = false;
    };
  }, []);

  useEffect(() => {
    if (!isAllowed) return;

    const controller = new AbortController();
    reloadPageData(controller.signal);
    return () => controller.abort();
  }, [isAllowed, reloadPageData]);

  const currentSettlementId = detail?.settlementId || settlementId || "-";
  const merchantId = detail?.merchantId || "-";
  const baseDate = detail?.baseDate || "-";
  const status = detail?.status || "UNKNOWN";
  const gross = detail?.gross ?? null;
  const fee = detail?.fee ?? null;
  const vat = detail?.vat ?? null;
  const net = detail?.net ?? null;
  const lines = Array.isArray(detail?.lines) ? detail.lines : [];
  const hold = detail?.hold ?? null;
  const refund = detail?.refund ?? null;

  const refundLineAmount = useMemo(() => {
    return lines
      .filter((line) => String(line?.type || "").toUpperCase() === "REFUND")
      .reduce((sum, line) => sum + Number(line?.amount || 0), 0);
  }, [lines]);

  const paymentIds = useMemo(() => {
    return [...new Set(lines.map((line) => line?.paymentId).filter(Boolean))];
  }, [lines]);

  const refundStatus = useMemo(() => {
    if (isRefundAdjustmentPending(refund)) return "REFUND_ADJUSTMENT_PENDING";
    if (hasApprovedRefund(refund)) return "APPROVED";
    return "NONE";
  }, [refund]);

  const holdActive =
    String(hold?.status || "").toUpperCase() === "HOLD_ACTIVE";

  const requestPaidDisabled = useMemo(() => {
    if (status !== "READY") return true;
    if (holdActive) return true;
    if (isRefundAdjustmentPending(refund)) return true;
    return false;
  }, [status, holdActive, refund]);

  const traceButtonDisabled = useMemo(() => {
    if (traceEntryLoading) return true;
    if (!traceRequestId) return true;
    return false;
  }, [traceEntryLoading, traceRequestId]);

  const guardMessages = useMemo(() => {
    const messages = [];

    if (holdActive) {
      messages.push(
        "Hold가 ACTIVE라 지급요청 불가입니다. Hold 큐(A5)에서 Release 후 다시 시도해야 합니다."
      );
    }

    if (isRefundAdjustmentPending(refund)) {
      messages.push(
        "승인된 환불이 있어 차감정산 반영 전입니다. 다음 배치 실행 후 다시 시도해야 합니다."
      );
    }

    if (status !== "READY") {
      messages.push("READY 상태에서만 지급 요청이 가능합니다.");
    }

    return messages;
  }, [holdActive, refund, status]);

  async function handleRequestPaid() {
    if (!isAllowed || !detail?.settlementId) return;

    try {
      setRequestPaidLoading(true);
      setActionMessage("");
      setActionError("");

      await requestAdminSettlementPaid(detail.settlementId);

      setActionMessage("지급 요청이 완료되었습니다.");
      await reloadPageData();
    } catch (error) {
      const statusCode = error?.status;
      const reason = error?.body?.reason;

      if (statusCode === 401) {
        setActionError(
          "인증이 만료되었거나 로그인되지 않았습니다. /auth/dev-login 에서 Admin 세션을 다시 생성해 주세요."
        );
      } else if (statusCode === 403) {
        setActionError("Admin 권한이 없어 지급 요청을 수행할 수 없습니다.");
      } else if (statusCode === 409) {
        setActionError(mapRequestPaidReasonToMessage(reason));
      } else {
        setActionError("지급 요청 중 오류가 발생했습니다.");
      }
    } finally {
      setRequestPaidLoading(false);
    }
  }

  function handleTraceEntry() {
    if (!isAllowed || !traceRequestId) return;
    navigate(`/admin/audit?requestId=${encodeURIComponent(traceRequestId)}`);
  }

  if (authChecking) {
    return (
      <PageLayout
        title="정산 상세"
        description="settlementId를 앵커로 settlement / settlement_line / hold / refund를 연결 조회하는 운영 허브입니다."
      >
        <SectionCard title="세션 확인 중">
          <LoadingBlock
            title="세션 확인 중"
            description="현재 로그인 세션의 역할을 확인하고 있습니다."
          />
        </SectionCard>
      </PageLayout>
    );
  }

  if (!isAllowed) {
    if (
      authErrorMessage.includes("로그인") ||
      authErrorMessage.includes("인증")
    ) {
      return (
        <PageLayout
          title="정산 상세"
          description="settlementId를 앵커로 settlement / settlement_line / hold / refund를 연결 조회하는 운영 허브입니다."
        >
          <RequireLoginNotice
            title="Admin 전용 화면"
            message={authErrorMessage}
          />
        </PageLayout>
      );
    }

    return (
      <PageLayout
        title="정산 상세"
        description="settlementId를 앵커로 settlement / settlement_line / hold / refund를 연결 조회하는 운영 허브입니다."
      >
        <GuardNotice
          title="접근 불가"
          message={authErrorMessage}
          tone="danger"
        />
        <ErrorState message={authErrorMessage} />
      </PageLayout>
    );
  }

  if (loading) {
    return (
      <PageLayout
        title="정산 상세"
        description="settlementId를 앵커로 settlement / settlement_line / hold / refund를 연결 조회하는 운영 허브입니다."
      >
        <SectionCard title="로딩 중">
          <LoadingBlock
            title="로딩 중"
            description="정산 상세 데이터를 불러오고 있습니다."
          />
        </SectionCard>
      </PageLayout>
    );
  }

  if (loadError) {
    return (
      <PageLayout
        title="정산 상세"
        description="settlementId를 앵커로 settlement / settlement_line / hold / refund를 연결 조회하는 운영 허브입니다."
      >
        <GuardNotice title="조회 실패" message={loadError} tone="danger" />
        <ErrorState message={loadError} />
      </PageLayout>
    );
  }

  if (!detail) {
    return (
      <PageLayout
        title="정산 상세"
        description="settlementId를 앵커로 settlement / settlement_line / hold / refund를 연결 조회하는 운영 허브입니다."
      >
        <GuardNotice
          title="데이터 없음"
          message="정산 정보를 찾을 수 없습니다."
          tone="warning"
        />
      </PageLayout>
    );
  }

  return (
    <PageLayout
      title="정산 상세"
      description="settlementId를 앵커로 settlement / settlement_line / hold / refund를 연결 조회하는 운영 허브입니다."
    >
      {guardMessages.length > 0 ? (
        <GuardNotice
          title="가드레일 안내"
          tone="warning"
          message={
            <div>
              {guardMessages.map((message) => (
                <div key={message}>{message}</div>
              ))}
            </div>
          }
        />
      ) : (
        <GuardNotice
          title="지급 요청 가능"
          tone="info"
          message="현재 HOLD_ACTIVE 및 REFUND_ADJUSTMENT_PENDING 조건이 없어 지급 요청이 가능합니다."
        />
      )}

      {actionMessage ? (
        <GuardNotice title="처리 완료" message={actionMessage} tone="success" />
      ) : null}

      {actionError ? (
        <GuardNotice title="요청 실패" message={actionError} tone="danger" />
      ) : null}

      {!traceEntryLoading && traceEntryError ? (
        <GuardNotice
          title="Trace 이동 불가"
          message={traceEntryError}
          tone="warning"
        />
      ) : null}

      <SectionCard title="상단 액션">
        <div className="action-panel">
          <ActionButton
            type="button"
            variant="primary"
            onClick={handleRequestPaid}
            disabled={requestPaidDisabled || requestPaidLoading}
          >
            {requestPaidLoading ? "요청 중…" : "지급 요청"}
          </ActionButton>

          <ActionButton
            type="button"
            variant="secondary"
            onClick={handleTraceEntry}
            disabled={traceButtonDisabled}
          >
            {traceEntryLoading ? "확인 중…" : "Trace로 보기"}
          </ActionButton>

          <Link
            to={buildHoldCreateLink(currentSettlementId)}
            className="btn btn--secondary"
          >
            Hold 생성
          </Link>

          <Link
            to={buildHoldQueueLink(currentSettlementId)}
            className="btn btn--secondary"
          >
            Hold 큐로 이동
          </Link>

          <Link
            to={buildRefundQueueLink(currentSettlementId)}
            className="btn btn--secondary"
          >
            Refund 큐로 이동
          </Link>

          <Link to="/admin/settlement-batches" className="btn btn--secondary">
            Batch 콘솔(A2)
          </Link>
        </div>

        <div style={{ marginTop: "12px" }}>
          {traceRequestId ? (
            <GuardNotice
              title="Trace 진입 정보"
              tone="info"
              message="아래 requestId를 복사하거나 ‘Trace로 보기’ 버튼으로 A1에서 요청 단위 재현을 확인할 수 있습니다."
            />
          ) : (
            <GuardNotice
              title="Trace 진입 정보"
              tone="info"
              message="현재 정산 건의 Trace 진입 requestId를 확인 중이거나, 아직 재현 가능한 requestId가 없습니다."
            />
          )}
        </div>
      </SectionCard>

      <SectionCard title="요약">
        <div className="summary-card-grid">
          <div className="card">
            <div className="card__body">
              <div className="summary-card__label">settlementId</div>
              <div className="summary-card__value">
                <CopyableValue value={currentSettlementId} />
              </div>
            </div>
          </div>

          <div className="card">
            <div className="card__body">
              <div className="summary-card__label">status</div>
              <div className="summary-card__value">
                <StatusBadge status={status} />
              </div>
            </div>
          </div>

          <div className="card">
            <div className="card__body">
              <div className="summary-card__label">merchantId</div>
              <div className="summary-card__value">
                <CopyableValue value={merchantId} />
              </div>
            </div>
          </div>

          <div className="card">
            <div className="card__body">
              <div className="summary-card__label">net</div>
              <div className="summary-card__value">{formatAmount(net)}</div>
            </div>
          </div>
        </div>
      </SectionCard>

      <div className="page-grid-2">
        <SectionCard title="정산 상세">
          <InfoRow label="baseDate" value={baseDate} />
          <InfoRow label="gross" value={formatAmount(gross)} />
          <InfoRow label="fee" value={formatAmount(fee)} />
          <InfoRow label="vat" value={formatAmount(vat)} />
          <InfoRow label="net" value={formatAmount(net)} />
        </SectionCard>

        <SectionCard title="운영 가드레일">
          <InfoRow label="hold">
            {hold?.status ? <StatusBadge status={hold.status} /> : "없음"}
          </InfoRow>
          <InfoRow
            label="approved refund"
            value={hasApprovedRefund(refund) ? "예" : "아니오"}
          />
          <InfoRow
            label="adjustment pending"
            value={isRefundAdjustmentPending(refund) ? "예" : "아니오"}
          />
          <InfoRow label="refund status" status value={refundStatus} />
        </SectionCard>
      </div>

      <SectionCard title="정산 수식 뷰">
        <div className="info-list">
          <div>[+] gross: {formatAmount(gross)}</div>
          <div>[-] fee: {formatAmount(fee)}</div>
          <div>[-] vat: {formatAmount(vat)}</div>
          <div>
            [-] refund:{" "}
            {refundLineAmount > 0 ? formatAmount(refundLineAmount) : "-"}
          </div>
          <div>
            <strong>[=] net: {formatAmount(net)}</strong>
          </div>
        </div>
      </SectionCard>

      <SectionCard title="연결 정보">
        <InfoRow label="settlementId" copyable value={currentSettlementId} />
        <InfoRow label="requestId" copyable value={traceRequestId || "-"} />
        <InfoRow label="merchantId" copyable value={merchantId} />

        <InfoRow label="paymentIds">
          {paymentIds.length === 0 ? (
            "-"
          ) : (
            <div
              style={{
                display: "flex",
                flexWrap: "wrap",
                gap: "8px",
              }}
            >
              {paymentIds.map((paymentId) => (
                <CopyableValue key={paymentId} value={paymentId} />
              ))}
            </div>
          )}
        </InfoRow>

        <InfoRow label="hold">
          {hold?.holdId ? <CopyableValue value={hold.holdId} /> : "없음"}
        </InfoRow>

        <InfoRow label="refund">
          {hasApprovedRefund(refund) ? (
            <StatusBadge status={refundStatus} />
          ) : (
            "없음"
          )}
        </InfoRow>
      </SectionCard>

      <div className="page-grid-2">
        <SectionCard title="Hold 요약">
          {!hold?.holdId ? (
            <GuardNotice
              title="현재 Hold 없음"
              message="이 정산 건에는 아직 Hold가 생성되지 않았습니다."
              tone="info"
            />
          ) : null}

          <InfoRow label="holdId">
            {hold?.holdId ? <CopyableValue value={hold.holdId} /> : "-"}
          </InfoRow>
          <InfoRow label="status">
            {hold?.status ? <StatusBadge status={hold.status} /> : "-"}
          </InfoRow>
          <InfoRow label="reason" value={hold?.reasonCode || "-"} />
          <InfoRow
            label="comment"
            value={hold?.requestedComment || hold?.comment || "-"}
          />
          <InfoRow
            label="approvedBy"
            value={hold?.approvedBy || hold?.decidedBy || "-"}
          />
          <InfoRow
            label="createdAt"
            value={hold?.createdAt || hold?.requestedAt || "-"}
          />
        </SectionCard>

        <SectionCard title="Refund 요약">
          <InfoRow
            label="approved refund"
            value={hasApprovedRefund(refund) ? "예" : "아니오"}
          />
          <InfoRow
            label="adjustment pending"
            value={isRefundAdjustmentPending(refund) ? "예" : "아니오"}
          />
          <InfoRow label="status" status value={refundStatus} />
          <InfoRow
            label="refund line amount"
            value={refundLineAmount > 0 ? formatAmount(refundLineAmount) : "-"}
          />
        </SectionCard>
      </div>

      <SectionCard title="라인 목록">
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
                  <td colSpan={4}>라인이 없습니다.</td>
                </tr>
              ) : (
                lines.map((line, index) => {
                  const lineId =
                    line?.settlementLineId || line?.lineId || `line-${index}`;

                  return (
                    <tr key={lineId}>
                      <td style={{ verticalAlign: "middle" }}>
                        <CopyableValue value={lineId} />
                      </td>
                      <td style={{ verticalAlign: "middle" }}>
                        <StatusBadge status={line?.type || "UNKNOWN"} />
                      </td>
                      <td style={{ verticalAlign: "middle" }}>
                        {line?.paymentId ? (
                          <CopyableValue value={line.paymentId} />
                        ) : (
                          "-"
                        )}
                      </td>
                      <td style={{ verticalAlign: "middle" }}>
                        {formatAmount(line?.amount)}
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </SectionCard>
    </PageLayout>
  );
}