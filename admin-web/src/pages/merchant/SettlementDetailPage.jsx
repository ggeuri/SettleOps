import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";

import PageLayout from "../../components/layout/PageLayout.jsx";
import SectionCard from "../../components/layout/SectionCard.jsx";
import StatusBadge from "../../components/display/StatusBadge.jsx";
import CopyableId from "../../components/display/CopyableId.jsx";
import InfoRow from "../../components/display/InfoRow.jsx";
import GuardNotice from "../../components/common/GuardNotice.jsx";
import LoadingBlock from "../../components/feedback/LoadingBlock.jsx";
import ErrorState from "../../components/feedback/ErrorState.jsx";
import RequireLoginNotice from "../../components/feedback/RequireLoginNotice.jsx";

import { getMe } from "../../api/meApi.js";
import { getMerchantSettlementDetail } from "../../api/merchantSettlementApi.js";

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

function isMerchantRole(payload) {
  const role = String(extractRole(payload) || "").toUpperCase();

  if (role === "MERCHANT" || role === "ROLE_MERCHANT") {
    return true;
  }

  if (
    Array.isArray(payload?.authorities) &&
    payload.authorities.includes("ROLE_MERCHANT")
  ) {
    return true;
  }

  return false;
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

function buildAuthErrorMessage(error) {
  const status = error?.status;
  const message = error?.body?.message || error?.body?.reason || error?.message;

  if (status === 401) {
    return "인증이 만료되었거나 로그인되지 않았습니다. /auth/dev-login 에서 Merchant 세션을 다시 생성해 주세요.";
  }

  if (status === 403) {
    return "Merchant 권한이 필요한 페이지입니다.";
  }

  return (
    message ||
    "현재 세션의 역할을 확인할 수 없습니다. /auth/dev-login 에서 Merchant 세션으로 다시 로그인해 주세요."
  );
}

function mapDetailErrorToMessage(error) {
  const status = error?.status;
  const message = error?.body?.message || error?.body?.reason || error?.message;

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

function CopyableValue({ value }) {
  if (!value || value === "-") {
    return <span>-</span>;
  }

  return <CopyableId value={value} short />;
}

export default function SettlementDetailPage() {
  const { settlementId } = useParams();

  const [detail, setDetail] = useState(null);
  const [merchantId, setMerchantId] = useState("");
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");

  const [authChecking, setAuthChecking] = useState(true);
  const [isAllowed, setIsAllowed] = useState(false);
  const [authErrorMessage, setAuthErrorMessage] = useState("");

  const fetchDetail = useCallback(
    async (resolvedMerchantId, signal) => {
      try {
        setLoading(true);
        setLoadError("");

        const data = await getMerchantSettlementDetail(
          resolvedMerchantId,
          settlementId,
          { signal }
        );

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

  useEffect(() => {
    let mounted = true;

    async function checkMerchantRole() {
      try {
        setAuthChecking(true);
        setAuthErrorMessage("");
        setIsAllowed(false);
        setMerchantId("");

        const me = await getMe();
        const resolvedMerchantId = extractMerchantId(me);

        if (!mounted) return;

        if (!isMerchantRole(me)) {
          setIsAllowed(false);
          setAuthErrorMessage("Merchant 권한이 필요한 페이지입니다.");
          return;
        }

        if (!resolvedMerchantId) {
          setIsAllowed(false);
          setAuthErrorMessage(
            "세션에서 merchantId를 확인할 수 없습니다. /auth/dev-login 에서 Merchant 세션을 다시 생성해 주세요."
          );
          return;
        }

        setMerchantId(resolvedMerchantId);
        setIsAllowed(true);
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

    checkMerchantRole();

    return () => {
      mounted = false;
    };
  }, []);

  useEffect(() => {
    if (!isAllowed || !merchantId) return;

    const controller = new AbortController();
    fetchDetail(merchantId, controller.signal);

    return () => controller.abort();
  }, [isAllowed, merchantId, fetchDetail]);

  const currentSettlementId = detail?.settlementId || settlementId || "-";
  const currentMerchantId = merchantId || "-";
  const baseDate = detail?.baseDate || "-";
  const status = detail?.status || "UNKNOWN";
  const gross = detail?.gross ?? null;
  const fee = detail?.fee ?? null;
  const vat = detail?.vat ?? null;
  const net = detail?.net ?? null;
  const lines = Array.isArray(detail?.lines) ? detail.lines : [];

  const refundLineAmount = useMemo(() => {
    return lines
      .filter((line) => String(line?.type || "").toUpperCase() === "REFUND")
      .reduce((sum, line) => sum + Number(line?.amount || 0), 0);
  }, [lines]);

  const paymentIds = useMemo(() => {
    return [...new Set(lines.map((line) => line?.paymentId).filter(Boolean))];
  }, [lines]);

  if (authChecking) {
    return (
      <PageLayout
        title="정산 상세"
        description="판매자 기준으로 settlement 상세와 라인 내역을 조회하는 화면입니다."
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
          description="판매자 기준으로 settlement 상세와 라인 내역을 조회하는 화면입니다."
        >
          <RequireLoginNotice
            title="Merchant 전용 화면"
            message={authErrorMessage}
          />
        </PageLayout>
      );
    }

    return (
      <PageLayout
        title="정산 상세"
        description="판매자 기준으로 settlement 상세와 라인 내역을 조회하는 화면입니다."
      >
        <div className="guard-notice">
          <div className="guard-notice__title">접근 불가</div>
          <div className="guard-notice__description">
            Merchant 권한이 필요한 페이지입니다.
          </div>
        </div>
      </PageLayout>
    );
  }

  if (loading) {
    return (
      <PageLayout
        title="정산 상세"
        description="판매자 기준으로 settlement 상세와 라인 내역을 조회하는 화면입니다."
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
        description="판매자 기준으로 settlement 상세와 라인 내역을 조회하는 화면입니다."
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
                <CopyableValue value={currentMerchantId} />
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

      <SectionCard title="정산 상세">
        <div className="page-grid-2">
          <div>
            <InfoRow label="baseDate" value={baseDate} />
            <InfoRow label="gross" value={formatAmount(gross)} />
            <InfoRow label="fee" value={formatAmount(fee)} />
            <InfoRow label="vat" value={formatAmount(vat)} />
            <InfoRow label="net" value={formatAmount(net)} />
          </div>

          <div>
            <InfoRow label="status">
              <StatusBadge status={status} />
            </InfoRow>
            <InfoRow label="settlementId">
              <CopyableValue value={currentSettlementId} />
            </InfoRow>
            <InfoRow label="merchantId">
              <CopyableValue value={currentMerchantId} />
            </InfoRow>
          </div>
        </div>
      </SectionCard>

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