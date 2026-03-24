import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";

import PageLayout from "../../components/layout/PageLayout.jsx";
import SectionCard from "../../components/layout/SectionCard.jsx";
import ActionButton from "../../components/layout/ActionButton.jsx";
import StatusBadge from "../../components/display/StatusBadge.jsx";
import CopyableId from "../../components/display/CopyableId.jsx";
import GuardNotice from "../../components/common/GuardNotice.jsx";
import EmptyState from "../../components/feedback/EmptyState.jsx";
import ErrorState from "../../components/feedback/ErrorState.jsx";
import LoadingBlock from "../../components/feedback/LoadingBlock.jsx";
import RequireLoginNotice from "../../components/feedback/RequireLoginNotice.jsx";

import { formatNumber } from "../../utils/format.js";
import { getMe } from "../../api/meApi.js";
import { getMerchantSettlements } from "../../api/merchantSettlementApi.js";

const STATUS_OPTIONS = [
  { value: "", label: "전체" },
  { value: "READY", label: "READY" },
  { value: "HOLD_ACTIVE", label: "HOLD_ACTIVE" },
  { value: "PAY_REQUESTED", label: "PAY_REQUESTED" },
  { value: "PAID", label: "PAID" },
];

function formatAmount(value) {
  if (value === null || value === undefined || value === "") return "-";
  const num = Number(value);
  if (Number.isNaN(num)) return String(value);
  return `${formatNumber(num)}원`;
}

function formatDate(date) {
  return date.toISOString().slice(0, 10);
}

function getDefaultFrom() {
  const date = new Date();
  date.setMonth(date.getMonth() - 1);
  return formatDate(date);
}

function getDefaultTo() {
  return formatDate(new Date());
}

function normalizeSettlementList(payload) {
  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload?.data)) return payload.data;
  if (Array.isArray(payload?.items)) return payload.items;
  if (Array.isArray(payload?.content)) return payload.content;
  return [];
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
  const message =
    error?.body?.message ||
    error?.body?.reason ||
    error?.message;

  if (status === 401) {
    return "인증이 만료되었거나 로그인되지 않았습니다. /auth/dev-login 에서 Merchant 세션을 다시 생성해 주세요.";
  }

  if (status === 403) {
    return "Merchant 권한이 없어 정산 리스트 화면에 접근할 수 없습니다.";
  }

  return (
    message ||
    "현재 세션의 역할을 확인할 수 없습니다. /auth/dev-login 에서 Merchant 세션으로 다시 로그인해 주세요."
  );
}

function buildErrorMessage(error) {
  const status = error?.status;
  const message =
    error?.body?.message ||
    error?.body?.reason ||
    error?.message;

  if (status === 401) {
    return "인증이 만료되었거나 로그인되지 않았습니다. /auth/dev-login 에서 Merchant 세션을 다시 생성해 주세요.";
  }

  if (status === 403) {
    return "Merchant 권한이 없어 정산 리스트를 조회할 수 없습니다.";
  }

  if (status === 404) {
    return "정산 리스트 API 경로를 찾을 수 없습니다. 백엔드 라우팅을 확인해 주세요.";
  }

  return (
    message ||
    "정산 리스트를 불러오지 못했습니다. 백엔드 연결 상태와 API 응답 구조를 확인해 주세요."
  );
}

export default function SettlementListPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const [rows, setRows] = useState([]);
  const [merchantId, setMerchantId] = useState("");
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  const [status, setStatus] = useState(searchParams.get("status") ?? "");
  const [fromDate, setFromDate] = useState(
    searchParams.get("from") ?? getDefaultFrom()
  );
  const [toDate, setToDate] = useState(
    searchParams.get("to") ?? getDefaultTo()
  );

  const [authChecking, setAuthChecking] = useState(true);
  const [isAllowed, setIsAllowed] = useState(false);
  const [authErrorMessage, setAuthErrorMessage] = useState("");

  const pageTitle = useMemo(() => "정산 리스트", []);

  const fetchSettlements = useCallback(
    async (resolvedMerchantId, nextStatus, nextFromDate, nextToDate) => {
      if (!resolvedMerchantId) return;

      setLoading(true);
      setErrorMessage("");

      try {
        const params = {};
        if (nextStatus) params.status = nextStatus;
        if (nextFromDate) params.from = nextFromDate;
        if (nextToDate) params.to = nextToDate;

        const data = await getMerchantSettlements(resolvedMerchantId, params);
        setRows(normalizeSettlementList(data));
      } catch (error) {
        console.error("U4 정산 리스트 조회 실패", error);
        setRows([]);
        setErrorMessage(buildErrorMessage(error));
      } finally {
        setLoading(false);
      }
    },
    []
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
        const role = String(extractRole(me) || "").toUpperCase();
        const resolvedMerchantId = extractMerchantId(me);

        if (!mounted) return;

        if (role !== "MERCHANT") {
          setIsAllowed(false);
          setAuthErrorMessage(
            "Merchant 전용 정산 리스트 화면입니다. /auth/dev-login 에서 Merchant 세션으로 다시 로그인해 주세요."
          );
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

    const nextStatus = searchParams.get("status") ?? "";
    const nextFromDate = searchParams.get("from") ?? getDefaultFrom();
    const nextToDate = searchParams.get("to") ?? getDefaultTo();

    setStatus(nextStatus);
    setFromDate(nextFromDate);
    setToDate(nextToDate);
    fetchSettlements(merchantId, nextStatus, nextFromDate, nextToDate);
  }, [searchParams, fetchSettlements, isAllowed, merchantId]);

  function handleSearch(event) {
    event.preventDefault();
    if (!isAllowed) return;

    const nextParams = {};
    if (status) nextParams.status = status;
    if (fromDate) nextParams.from = fromDate;
    if (toDate) nextParams.to = toDate;

    setSearchParams(nextParams);
  }

  function handleReset() {
    const nextFrom = getDefaultFrom();
    const nextTo = getDefaultTo();

    setStatus("");
    setFromDate(nextFrom);
    setToDate(nextTo);

    if (!isAllowed) return;

    setSearchParams({
      from: nextFrom,
      to: nextTo,
    });
  }

  function handleRowClick(settlementId) {
    if (!isAllowed || !settlementId) return;
    navigate(`/merchant/settlements/${settlementId}`);
  }

  if (authChecking) {
    return (
      <PageLayout
        title={pageTitle}
        description="판매자 기준으로 정산 내역을 조회하고 settlementId 앵커로 상세 화면(U5)으로 이동합니다."
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
          title={pageTitle}
          description="판매자 기준으로 정산 내역을 조회하고 settlementId 앵커로 상세 화면(U5)으로 이동합니다."
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
        title={pageTitle}
        description="판매자 기준으로 정산 내역을 조회하고 settlementId 앵커로 상세 화면(U5)으로 이동합니다."
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

  return (
    <PageLayout
      title={pageTitle}
      description="판매자 기준으로 정산 내역을 조회하고 settlementId 앵커로 상세 화면(U5)으로 이동합니다."
    >
      <SectionCard title="조회 조건">
        <form onSubmit={handleSearch}>
          <div
            className="filter-bar"
            style={{
              display: "flex",
              flexWrap: "wrap",
              alignItems: "flex-end",
              gap: "16px",
            }}
          >
            <div
              className="form-field"
              style={{ minWidth: "220px", flex: "0 0 220px" }}
            >
              <label className="form-field__label" htmlFor="status">
                상태
              </label>
              <select
                id="status"
                className="select"
                value={status}
                onChange={(event) => setStatus(event.target.value)}
              >
                {STATUS_OPTIONS.map((option) => (
                  <option key={option.value || "ALL"} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </div>

            <div
              className="form-field"
              style={{ minWidth: "220px", flex: "0 0 220px" }}
            >
              <label className="form-field__label" htmlFor="fromDate">
                from
              </label>
              <input
                id="fromDate"
                className="input"
                type="date"
                value={fromDate}
                onChange={(event) => setFromDate(event.target.value)}
              />
            </div>

            <div
              className="form-field"
              style={{ minWidth: "220px", flex: "0 0 220px" }}
            >
              <label className="form-field__label" htmlFor="toDate">
                to
              </label>
              <input
                id="toDate"
                className="input"
                type="date"
                value={toDate}
                onChange={(event) => setToDate(event.target.value)}
              />
            </div>

            <div
              className="button-row action-panel"
              style={{
                display: "flex",
                alignItems: "center",
                gap: "12px",
                flexWrap: "wrap",
              }}
            >
              <ActionButton
                type="submit"
                variant="primary"
                disabled={loading}
              >
                {loading ? "조회 중..." : "조회"}
              </ActionButton>

              <ActionButton
                type="button"
                variant="secondary"
                onClick={handleReset}
                disabled={loading}
              >
                초기화
              </ActionButton>

              <Link to="/merchant/refunds" className="btn btn--secondary">
                환불 현황(U6)
              </Link>
            </div>
          </div>
        </form>
      </SectionCard>

      {merchantId ? (
        <GuardNotice
          title="현재 판매자 기준"
          tone="info"
          message={
            <span>
              merchantId <CopyableId value={merchantId} short />
            </span>
          }
        />
      ) : null}

      {errorMessage ? (
        <>
          <GuardNotice
            title="조회 실패"
            message={errorMessage}
            tone="danger"
          />
          <ErrorState message={errorMessage} />
        </>
      ) : null}

      <SectionCard title={`정산 리스트 (${rows.length}건)`}>
        {loading ? (
          <LoadingBlock
            title="로딩 중"
            description="정산 리스트를 불러오고 있습니다."
          />
        ) : rows.length === 0 ? (
          <EmptyState
            title="조회 결과가 없습니다"
            description="검색 조건을 다시 확인하거나 기간을 넓혀 주세요."
          />
        ) : (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>settlementId</th>
                  <th>상태</th>
                  <th>baseDate</th>
                  <th>gross</th>
                  <th>fee</th>
                  <th>vat</th>
                  <th>net</th>
                  <th>상세</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row, index) => {
                  const settlementId =
                    row?.settlementId ?? row?.id ?? `row-${index}`;

                  return (
                    <tr
                      key={settlementId}
                      onClick={() => handleRowClick(settlementId)}
                      style={{ cursor: settlementId ? "pointer" : "default" }}
                    >
                      <td
                        onClick={(event) => event.stopPropagation()}
                        style={{ verticalAlign: "middle" }}
                      >
                        <CopyableId value={settlementId} short />
                      </td>

                      <td style={{ verticalAlign: "middle" }}>
                        <StatusBadge status={row?.status || "UNKNOWN"} />
                      </td>

                      <td style={{ verticalAlign: "middle" }}>
                        {row?.baseDate ?? "-"}
                      </td>

                      <td style={{ verticalAlign: "middle" }}>
                        {formatAmount(row?.gross)}
                      </td>

                      <td style={{ verticalAlign: "middle" }}>
                        {formatAmount(row?.fee)}
                      </td>

                      <td style={{ verticalAlign: "middle" }}>
                        {formatAmount(row?.vat)}
                      </td>

                      <td style={{ verticalAlign: "middle" }}>
                        {formatAmount(row?.net)}
                      </td>

                      <td
                        onClick={(event) => event.stopPropagation()}
                        style={{ verticalAlign: "middle" }}
                      >
                        <ActionButton
                          type="button"
                          variant="secondary"
                          onClick={() => handleRowClick(settlementId)}
                          disabled={!settlementId}
                        >
                          상세조회
                        </ActionButton>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </SectionCard>
    </PageLayout>
  );
}