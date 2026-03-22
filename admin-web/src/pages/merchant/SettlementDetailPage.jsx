// /admin-web/src/pages/merchant/SettlementDetailPage.jsx

import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import axios from "axios";

import PageLayout from "../../components/layout/PageLayout.jsx";
import SectionCard from "../../components/layout/SectionCard.jsx";
import StatusBadge from "../../components/display/StatusBadge.jsx";
import CopyableId from "../../components/display/CopyableId.jsx";
import GuardNotice from "../../components/common/GuardNotice.jsx";

function formatAmount(value) {
  if (value === null || value === undefined || value === "") return "-";
  const num = Number(value);
  if (Number.isNaN(num)) return String(value);
  return `${num.toLocaleString("ko-KR")}원`;
}

function mapDetailErrorToMessage(error) {
  const status = error?.response?.status;
  const message =
    error?.response?.data?.message ||
    error?.response?.data?.reason ||
    error?.message;

  if (status === 401) {
    return "인증이 만료되었거나 로그인되지 않았습니다. /auth/dev-login 에서 Merchant 세션을 다시 생성해 주세요.";
  }
  if (status === 403) {
    return "Merchant 권한이 없어 정산 상세를 조회할 수 없습니다.";
  }
  if (status === 404) {
    return "정산 정보를 찾을 수 없습니다.";
  }
  return message || "정산 상세 조회 중 오류가 발생했습니다.";
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

function extractMerchantId(payload) {
  return (
    payload?.merchantId ||
    payload?.data?.merchantId ||
    payload?.user?.merchantId ||
    payload?.principal?.merchantId ||
    null
  );
}

function InlineCopyValue({ value }) {
  if (!value || value === "-") {
    return <span>-</span>;
  }

  return (
    <span
      style={{
        display: "inline-flex",
        alignItems: "center",
        maxWidth: "100%",
        verticalAlign: "middle",
      }}
    >
      <CopyableId value={value} short />
    </span>
  );
}

function DetailRow({ label, value }) {
  return (
    <div
      style={{
        display: "grid",
        gridTemplateColumns: "160px 1fr",
        gap: "12px",
        alignItems: "start",
        padding: "6px 0",
      }}
    >
      <div
        style={{
          color: "var(--color-text-muted, #6b7280)",
          fontWeight: 600,
          lineHeight: 1.5,
        }}
      >
        {label}
      </div>
      <div style={{ lineHeight: 1.5, minWidth: 0 }}>{value}</div>
    </div>
  );
}

export default function SettlementDetailPage() {
  const { settlementId } = useParams();

  const [detail, setDetail] = useState(null);
  const [merchantId, setMerchantId] = useState("");
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");

  const fetchMerchantId = useCallback(async () => {
    const response = await axios.get("/api/me", {
      withCredentials: true,
    });

    const resolvedMerchantId = extractMerchantId(response.data);

    if (!resolvedMerchantId) {
      throw new Error("세션에서 merchantId를 확인할 수 없습니다.");
    }

    return resolvedMerchantId;
  }, []);

  const fetchDetail = useCallback(
    async (signal) => {
      try {
        setLoading(true);
        setLoadError("");

        const resolvedMerchantId = merchantId || (await fetchMerchantId());
        setMerchantId(resolvedMerchantId);

        const response = await axios.get(
          `/api/merchants/${encodeURIComponent(
            resolvedMerchantId
          )}/settlements/${settlementId}`,
          {
            withCredentials: true,
            signal,
          }
        );

        setDetail(response.data || null);
      } catch (error) {
        if (error?.name === "CanceledError" || error?.code === "ERR_CANCELED") {
          return;
        }
        setDetail(null);
        setLoadError(mapDetailErrorToMessage(error));
      } finally {
        setLoading(false);
      }
    },
    [fetchMerchantId, merchantId, settlementId]
  );

  useEffect(() => {
    const controller = new AbortController();
    fetchDetail(controller.signal);
    return () => controller.abort();
  }, [fetchDetail]);

  const currentSettlementId = detail?.settlementId || settlementId || "-";
  const currentMerchantId = detail?.merchantId || merchantId || "-";
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

  if (loading) {
    return (
      <PageLayout
        title="정산 상세"
        description="판매자 기준으로 settlement 상세와 라인 내역을 조회하는 화면입니다."
      >
        <SectionCard title="로딩 중">
          <div className="state-block">
            <div className="state-block__title">로딩 중</div>
            <div className="state-block__description">
              정산 상세 데이터를 불러오고 있습니다.
            </div>
          </div>
        </SectionCard>
      </PageLayout>
    );
  }

  if (loadError) {
    return (
      <PageLayout
        title="정산 상세"
        description="판매자 기준으로 settlement 상세와 라인 내역을 조회하는 화면입니다."
      >
        <GuardNotice title="조회 실패" message={loadError} tone="danger" />
      </PageLayout>
    );
  }

  if (!detail) {
    return (
      <PageLayout
        title="정산 상세"
        description="판매자 기준으로 settlement 상세와 라인 내역을 조회하는 화면입니다."
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
      description="판매자 기준으로 settlement 상세와 라인 내역을 조회하는 화면입니다."
    >
      <SectionCard title="상단 액션">
        <div className="action-panel">
          <Link to="/merchant/settlements" className="btn btn--secondary">
            정산 리스트(U4)
          </Link>

          <Link to="/merchant/refunds" className="btn btn--secondary">
            환불 현황(U6)
          </Link>
        </div>
      </SectionCard>

      <SectionCard title="요약">
        <div className="summary-card-grid">
          <div className="card">
            <div className="card__body">
              <div className="summary-card__label">settlementId</div>
              <div className="summary-card__value">
                <InlineCopyValue value={currentSettlementId} />
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
                <InlineCopyValue value={currentMerchantId} />
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
          <DetailRow label="baseDate" value={baseDate} />
          <DetailRow label="gross" value={formatAmount(gross)} />
          <DetailRow label="fee" value={formatAmount(fee)} />
          <DetailRow label="vat" value={formatAmount(vat)} />
          <DetailRow label="net" value={formatAmount(net)} />
        </SectionCard>

        <SectionCard title="정산 상태 참고">
          <DetailRow
            label="hold"
            value={hold?.status ? <StatusBadge status={hold.status} /> : "없음"}
          />
          <DetailRow
            label="approved refund"
            value={hasApprovedRefund(refund) ? "예" : "아니오"}
          />
          <DetailRow
            label="adjustment pending"
            value={isRefundAdjustmentPending(refund) ? "예" : "아니오"}
          />
          <DetailRow
            label="refund status"
            value={<StatusBadge status={refundStatus} />}
          />
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
        <div
          style={{
            display: "grid",
            gap: "12px",
          }}
        >
          <DetailRow
            label="settlementId"
            value={<InlineCopyValue value={currentSettlementId} />}
          />

          <DetailRow
            label="merchantId"
            value={
              currentMerchantId !== "-" ? (
                <InlineCopyValue value={currentMerchantId} />
              ) : (
                "-"
              )
            }
          />

          <DetailRow
            label="paymentIds"
            value={
              paymentIds.length === 0 ? (
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
                    <InlineCopyValue key={paymentId} value={paymentId} />
                  ))}
                </div>
              )
            }
          />

          <DetailRow
            label="hold"
            value={hold?.holdId ? <InlineCopyValue value={hold.holdId} /> : "없음"}
          />

          <DetailRow
            label="refund"
            value={
              hasApprovedRefund(refund) ? (
                <StatusBadge status={refundStatus} />
              ) : (
                "없음"
              )
            }
          />
        </div>
      </SectionCard>

      <div className="page-grid-2">
        <SectionCard title="Hold 요약">
          <DetailRow
            label="holdId"
            value={hold?.holdId ? <InlineCopyValue value={hold.holdId} /> : "-"}
          />
          <DetailRow
            label="status"
            value={hold?.status ? <StatusBadge status={hold.status} /> : "-"}
          />
          <DetailRow label="reason" value={hold?.reasonCode || "-"} />
          <DetailRow
            label="comment"
            value={hold?.requestedComment || hold?.comment || "-"}
          />
          <DetailRow
            label="approvedBy"
            value={hold?.approvedBy || hold?.decidedBy || "-"}
          />
          <DetailRow
            label="createdAt"
            value={hold?.createdAt || hold?.requestedAt || "-"}
          />
        </SectionCard>

        <SectionCard title="Refund 요약">
          <DetailRow
            label="approved refund"
            value={hasApprovedRefund(refund) ? "예" : "아니오"}
          />
          <DetailRow
            label="adjustment pending"
            value={isRefundAdjustmentPending(refund) ? "예" : "아니오"}
          />
          <DetailRow
            label="status"
            value={<StatusBadge status={refundStatus} />}
          />
          <DetailRow
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
                        <InlineCopyValue value={lineId} />
                      </td>
                      <td style={{ verticalAlign: "middle" }}>
                        <StatusBadge status={line?.type || "UNKNOWN"} />
                      </td>
                      <td style={{ verticalAlign: "middle" }}>
                        {line?.paymentId ? (
                          <InlineCopyValue value={line.paymentId} />
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