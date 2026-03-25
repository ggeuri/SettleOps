import { useEffect, useMemo, useState } from "react";
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
import Pagination from "../../components/table/Pagination.jsx";
import usePagination from "../../hooks/usePagination.js";

import { formatNumber } from "../../utils/format.js";
import { getMe } from "../../api/meApi.js";
import { getMerchantSettlements } from "../../api/merchantSettlementApi.js";

const DEFAULT_PAGE = 0;
const DEFAULT_SIZE = 20;

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
  date.setDate(date.getDate() - 7);
  return formatDate(date);
}

function getDefaultTo() {
  return formatDate(new Date());
}

function normalizeSettlementPage(payload) {
  if (Array.isArray(payload?.items)) {
    return {
      items: payload.items,
      page: Number(payload.page ?? DEFAULT_PAGE),
      size: Number(payload.size ?? DEFAULT_SIZE),
      totalElements: Number(payload.totalElements ?? payload.items.length ?? 0),
      totalPages: Number(
        payload.totalPages ?? (payload.items.length > 0 ? 1 : 0)
      ),
      hasNext: Boolean(payload.hasNext),
      hasPrevious: Boolean(payload.hasPrevious),
    };
  }

  if (Array.isArray(payload?.content)) {
    return {
      items: payload.content,
      page: Number(payload.number ?? payload.page ?? DEFAULT_PAGE),
      size: Number(payload.size ?? DEFAULT_SIZE),
      totalElements: Number(
        payload.totalElements ?? payload.content.length ?? 0
      ),
      totalPages: Number(
        payload.totalPages ?? (payload.content.length > 0 ? 1 : 0)
      ),
      hasNext: Boolean(payload.hasNext),
      hasPrevious: Boolean(payload.hasPrevious),
    };
  }

  if (Array.isArray(payload?.page?.content)) {
    return {
      items: payload.page.content,
      page: Number(payload.page.number ?? DEFAULT_PAGE),
      size: Number(payload.page.size ?? DEFAULT_SIZE),
      totalElements: Number(
        payload.page.totalElements ?? payload.page.content.length ?? 0
      ),
      totalPages: Number(
        payload.page.totalPages ?? (payload.page.content.length > 0 ? 1 : 0)
      ),
      hasNext: Boolean(payload.page.hasNext),
      hasPrevious: Boolean(payload.page.hasPrevious),
    };
  }

  if (Array.isArray(payload?.data)) {
    return {
      items: payload.data,
      page: DEFAULT_PAGE,
      size: DEFAULT_SIZE,
      totalElements: payload.data.length,
      totalPages: payload.data.length > 0 ? 1 : 0,
      hasNext: false,
      hasPrevious: false,
    };
  }

  return {
    items: [],
    page: DEFAULT_PAGE,
    size: DEFAULT_SIZE,
    totalElements: 0,
    totalPages: 0,
    hasNext: false,
    hasPrevious: false,
  };
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

function buildAuthErrorInfo(error) {
  const status = error?.status ?? null;
  const message =
    error?.body?.message ||
    error?.body?.reason ||
    error?.message;

  if (status === 401) {
    return {
      status: 401,
      message:
        "로그인이 필요합니다. 개발용 로그인 페이지에서 세션을 생성해주세요.",
    };
  }

  if (status === 403) {
    return {
      status: 403,
      message: "Merchant 권한이 필요한 페이지입니다.",
    };
  }

  return {
    status,
    message:
      message ||
      "현재 세션의 역할을 확인할 수 없습니다. /auth/dev-login 에서 Merchant 세션으로 다시 로그인해 주세요.",
  };
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

  const initialStatus = searchParams.get("status") ?? "";
  const initialFrom = searchParams.get("from") ?? getDefaultFrom();
  const initialTo = searchParams.get("to") ?? getDefaultTo();
  const initialPage = Number(searchParams.get("page") ?? DEFAULT_PAGE);
  const initialSize = Number(searchParams.get("size") ?? DEFAULT_SIZE);

  const safeInitialPage = Number.isNaN(initialPage) ? DEFAULT_PAGE : initialPage;
  const safeInitialSize = Number.isNaN(initialSize) ? DEFAULT_SIZE : initialSize;

  const { page, size, setPage, setSize, resetPage } = usePagination(
    safeInitialPage,
    safeInitialSize
  );

  const [rows, setRows] = useState([]);
  const [merchantId, setMerchantId] = useState("");
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  const [pageInfo, setPageInfo] = useState({
    page: DEFAULT_PAGE,
    size: DEFAULT_SIZE,
    totalElements: 0,
    totalPages: 0,
    hasNext: false,
    hasPrevious: false,
  });

  const [status, setStatus] = useState(initialStatus);
  const [fromDate, setFromDate] = useState(initialFrom);
  const [toDate, setToDate] = useState(initialTo);

  const [authChecking, setAuthChecking] = useState(true);
  const [isAllowed, setIsAllowed] = useState(false);
  const [authErrorInfo, setAuthErrorInfo] = useState(null);

  const pageTitle = useMemo(() => "정산 리스트", []);

  function applyPageResponse(response, fallbackSize = size) {
    setRows(response?.items ?? []);
    setPageInfo({
      page: response?.page ?? DEFAULT_PAGE,
      size: response?.size ?? fallbackSize,
      totalElements: response?.totalElements ?? 0,
      totalPages: response?.totalPages ?? 0,
      hasNext: response?.hasNext ?? false,
      hasPrevious: response?.hasPrevious ?? false,
    });
  }

  function clearPageResponse(nextSize = size) {
    setRows([]);
    setPageInfo({
      page: DEFAULT_PAGE,
      size: nextSize,
      totalElements: 0,
      totalPages: 0,
      hasNext: false,
      hasPrevious: false,
    });
  }

  async function loadSettlements(
    resolvedMerchantId,
    nextStatus = status,
    nextFromDate = fromDate,
    nextToDate = toDate,
    nextPage = page,
    nextSize = size
  ) {
    if (!resolvedMerchantId) {
      clearPageResponse(nextSize);
      return;
    }

    const params = {
      page: nextPage,
      size: nextSize,
    };

    if (nextStatus) params.status = nextStatus;
    if (nextFromDate) params.from = nextFromDate;
    if (nextToDate) params.to = nextToDate;

    const data = await getMerchantSettlements(resolvedMerchantId, params);
    const normalized = normalizeSettlementPage(data);

    applyPageResponse(normalized, nextSize);
  }

  async function runSettlementsLoad(
    resolvedMerchantId,
    nextStatus = status,
    nextFromDate = fromDate,
    nextToDate = toDate,
    nextPage = page,
    nextSize = size
  ) {
    try {
      setLoading(true);
      setErrorMessage("");
      await loadSettlements(
        resolvedMerchantId,
        nextStatus,
        nextFromDate,
        nextToDate,
        nextPage,
        nextSize
      );
    } catch (error) {
      console.error("U4 정산 리스트 조회 실패", error);
      clearPageResponse(nextSize);
      setErrorMessage(buildErrorMessage(error));
    } finally {
      setLoading(false);
    }
  }

  function syncSearchParams(
    nextStatus,
    nextFromDate,
    nextToDate,
    nextPage,
    nextSize
  ) {
    const nextParams = {};
    if (nextStatus) nextParams.status = nextStatus;
    if (nextFromDate) nextParams.from = nextFromDate;
    if (nextToDate) nextParams.to = nextToDate;
    nextParams.page = String(nextPage);
    nextParams.size = String(nextSize);
    setSearchParams(nextParams);
  }

  useEffect(() => {
    async function loadPage() {
      try {
        setAuthChecking(true);
        setAuthErrorInfo(null);
        setIsAllowed(false);
        setMerchantId("");
        setErrorMessage("");

        const me = await getMe();
        const resolvedMerchantId = extractMerchantId(me);

        if (!isMerchantRole(me)) {
          setIsAllowed(false);
          setAuthErrorInfo({
            status: 403,
            message: "Merchant 권한이 필요한 페이지입니다.",
          });
          clearPageResponse(size);
          return;
        }

        if (!resolvedMerchantId) {
          setIsAllowed(false);
          setAuthErrorInfo({
            status: 403,
            message:
              "세션에서 merchantId를 확인할 수 없습니다. /auth/dev-login 에서 Merchant 세션을 다시 생성해 주세요.",
          });
          clearPageResponse(size);
          return;
        }

        setMerchantId(resolvedMerchantId);
        setIsAllowed(true);

        await runSettlementsLoad(
          resolvedMerchantId,
          initialStatus,
          initialFrom,
          initialTo,
          safeInitialPage,
          safeInitialSize
        );
      } catch (error) {
        setIsAllowed(false);
        setMerchantId("");
        setAuthErrorInfo(buildAuthErrorInfo(error));
        clearPageResponse(size);
      } finally {
        setAuthChecking(false);
      }
    }

    loadPage();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function handleSearch(event) {
    event.preventDefault();
    if (!isAllowed || !merchantId) return;

    const nextPage = DEFAULT_PAGE;
    const nextSize = size;

    resetPage();
    await runSettlementsLoad(
      merchantId,
      status,
      fromDate,
      toDate,
      nextPage,
      nextSize
    );
    syncSearchParams(status, fromDate, toDate, nextPage, nextSize);
  }

  async function handleReset() {
    if (!isAllowed || !merchantId) return;

    const nextStatus = "";
    const nextFrom = getDefaultFrom();
    const nextTo = getDefaultTo();
    const nextPage = DEFAULT_PAGE;
    const nextSize = DEFAULT_SIZE;

    setStatus(nextStatus);
    setFromDate(nextFrom);
    setToDate(nextTo);
    setSize(nextSize);
    setPage(nextPage);
    resetPage();

    await runSettlementsLoad(
      merchantId,
      nextStatus,
      nextFrom,
      nextTo,
      nextPage,
      nextSize
    );
    syncSearchParams(nextStatus, nextFrom, nextTo, nextPage, nextSize);
  }

  function handleRowClick(settlementId) {
    if (!isAllowed || !settlementId) return;
    navigate(`/merchant/settlements/${settlementId}`);
  }

  async function handlePageChange(nextPage) {
    if (!isAllowed || !merchantId) return;

    setPage(nextPage);
    await runSettlementsLoad(
      merchantId,
      status,
      fromDate,
      toDate,
      nextPage,
      size
    );
    syncSearchParams(status, fromDate, toDate, nextPage, size);
  }

  async function handleSizeChange(event) {
    if (!isAllowed || !merchantId) return;

    const nextSize = Number(event.target.value);
    const nextPage = DEFAULT_PAGE;

    setSize(nextSize);
    resetPage();

    await runSettlementsLoad(
      merchantId,
      status,
      fromDate,
      toDate,
      nextPage,
      nextSize
    );
    syncSearchParams(status, fromDate, toDate, nextPage, nextSize);
  }

  if (authChecking) {
    return (
      <PageLayout
        title={pageTitle}
        description="판매자 기준으로 정산 내역을 조회하고 settlementId 앵커로 상세 화면(U5)으로 이동합니다."
      >
        <div className="guard-notice">
          <div className="guard-notice__title">로딩 중</div>
          <div className="guard-notice__description">
            권한 정보를 확인하는 중입니다.
          </div>
        </div>
      </PageLayout>
    );
  }

  if (authErrorInfo?.status === 401) {
    return (
      <PageLayout
        title={pageTitle}
        description="판매자 기준으로 정산 내역을 조회하고 settlementId 앵커로 상세 화면(U5)으로 이동합니다."
      >
        <RequireLoginNotice
          title="인증 필요"
          message={authErrorInfo.message}
        />
      </PageLayout>
    );
  }

  if (!isAllowed) {
    return (
      <PageLayout
        title={pageTitle}
        description="판매자 기준으로 정산 내역을 조회하고 settlementId 앵커로 상세 화면(U5)으로 이동합니다."
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

      <SectionCard title="정산 리스트">
        <div className="table-toolbar">
          <div>정산 리스트 · 총 {formatNumber(pageInfo.totalElements)}건</div>

          <div className="action-panel">
            <select className="select" value={size} onChange={handleSizeChange}>
              <option value={10}>10개</option>
              <option value={20}>20개</option>
              <option value={50}>50개</option>
            </select>
          </div>
        </div>

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
          <>
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

            <Pagination
              page={pageInfo.page}
              totalPages={pageInfo.totalPages}
              onPageChange={handlePageChange}
              disabled={loading}
            />
          </>
        )}
      </SectionCard>
    </PageLayout>
  );
}