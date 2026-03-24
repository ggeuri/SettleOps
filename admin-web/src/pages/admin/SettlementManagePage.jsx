import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate, Link, useSearchParams } from "react-router-dom";

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
import { getAdminSettlements } from "../../api/adminSettlementListApi.js";

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

function normalizeSettlementResponse(payload) {
  if (Array.isArray(payload)) {
    return {
      items: payload,
      page: DEFAULT_PAGE,
      size: payload.length || DEFAULT_SIZE,
      totalElements: payload.length,
      totalPages: payload.length > 0 ? 1 : 0,
      hasNext: false,
      hasPrevious: false,
    };
  }

  if (Array.isArray(payload?.items)) {
    return {
      items: payload.items,
      page: Number(payload.page ?? DEFAULT_PAGE),
      size: Number(payload.size ?? DEFAULT_SIZE),
      totalElements: Number(payload.totalElements ?? payload.items.length ?? 0),
      totalPages: Number(payload.totalPages ?? (payload.items.length > 0 ? 1 : 0)),
      hasNext: Boolean(payload.hasNext),
      hasPrevious: Boolean(payload.hasPrevious),
    };
  }

  if (Array.isArray(payload?.content)) {
    return {
      items: payload.content,
      page: Number(payload.number ?? payload.page ?? DEFAULT_PAGE),
      size: Number(payload.size ?? DEFAULT_SIZE),
      totalElements: Number(payload.totalElements ?? payload.content.length ?? 0),
      totalPages: Number(payload.totalPages ?? (payload.content.length > 0 ? 1 : 0)),
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
      size: payload.data.length || DEFAULT_SIZE,
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

function buildAuthErrorMessage(error) {
  const status = error?.status;
  const message =
    error?.body?.message ||
    error?.body?.reason ||
    error?.message;

  if (status === 401) {
    return "로그인이 필요합니다. 개발용 로그인 페이지에서 세션을 생성해주세요.";
  }

  if (status === 403) {
    return "Admin 권한이 없어 정산 관리 화면에 접근할 수 없습니다.";
  }

  return (
    message ||
    "현재 세션의 역할을 확인할 수 없습니다. /auth/dev-login 에서 Admin 세션으로 다시 로그인해 주세요."
  );
}

function buildErrorMessage(error) {
  const status = error?.status;
  const message =
    error?.body?.message ||
    error?.body?.reason ||
    error?.message;

  if (status === 401) {
    return "인증이 만료되었거나 로그인되지 않았습니다. /auth/dev-login 에서 Admin 세션을 다시 생성해 주세요.";
  }

  if (status === 403) {
    return "Admin 권한이 없어 정산 리스트를 조회할 수 없습니다.";
  }

  if (status === 404) {
    return "정산 리스트 API 경로를 찾을 수 없습니다. 백엔드 라우팅을 확인해 주세요.";
  }

  return (
    message ||
    "정산 리스트를 불러오지 못했습니다. 백엔드 연결 상태와 API 응답 구조를 확인해 주세요."
  );
}

export default function SettlementManagePage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const [rows, setRows] = useState([]);
  const [page, setPage] = useState(DEFAULT_PAGE);
  const [size, setSize] = useState(DEFAULT_SIZE);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [hasNext, setHasNext] = useState(false);
  const [hasPrevious, setHasPrevious] = useState(false);

  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  const [status, setStatus] = useState(searchParams.get("status") ?? "");
  const [merchantId, setMerchantId] = useState(
    searchParams.get("merchantId") ?? ""
  );

  const [authChecking, setAuthChecking] = useState(true);
  const [isAllowed, setIsAllowed] = useState(false);
  const [authErrorMessage, setAuthErrorMessage] = useState("");

  const pageTitle = useMemo(() => "A3 정산 관리", []);

  const fetchSettlements = useCallback(
    async ({
      nextStatus = "",
      nextMerchantId = "",
      nextPage = DEFAULT_PAGE,
      nextSize = DEFAULT_SIZE,
    }) => {
      setLoading(true);
      setErrorMessage("");

      try {
        const params = {
          page: nextPage,
          size: nextSize,
        };

        if (nextStatus) params.status = nextStatus;
        if (nextMerchantId.trim()) params.merchantId = nextMerchantId.trim();

        const data = await getAdminSettlements(params);
        const normalized = normalizeSettlementResponse(data);

        setRows(normalized.items);
        setPage(normalized.page);
        setSize(normalized.size);
        setTotalElements(normalized.totalElements);
        setTotalPages(normalized.totalPages);
        setHasNext(normalized.hasNext);
        setHasPrevious(normalized.hasPrevious);
      } catch (error) {
        console.error("정산 리스트 조회 실패", error);
        setRows([]);
        setPage(DEFAULT_PAGE);
        setSize(DEFAULT_SIZE);
        setTotalElements(0);
        setTotalPages(0);
        setHasNext(false);
        setHasPrevious(false);
        setErrorMessage(buildErrorMessage(error));
      } finally {
        setLoading(false);
      }
    },
    []
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
          "Admin 전용 정산 관리 화면입니다. /auth/dev-login 에서 Admin 세션으로 다시 로그인해 주세요."
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

    const nextStatus = searchParams.get("status") ?? "";
    const nextMerchantId = searchParams.get("merchantId") ?? "";
    const nextPage = Number(searchParams.get("page") ?? DEFAULT_PAGE);
    const nextSize = Number(searchParams.get("size") ?? DEFAULT_SIZE);

    setStatus(nextStatus);
    setMerchantId(nextMerchantId);

    fetchSettlements({
      nextStatus,
      nextMerchantId,
      nextPage,
      nextSize,
    });
  }, [searchParams, fetchSettlements, isAllowed]);

  function handleSearch(event) {
    event.preventDefault();

    const nextParams = {};
    if (status) nextParams.status = status;
    if (merchantId.trim()) nextParams.merchantId = merchantId.trim();
    nextParams.page = String(DEFAULT_PAGE);
    nextParams.size = String(size || DEFAULT_SIZE);

    setSearchParams(nextParams);
  }

  function handleReset() {
    setStatus("");
    setMerchantId("");
    setSearchParams({
      page: String(DEFAULT_PAGE),
      size: String(DEFAULT_SIZE),
    });
  }

  function handlePageChange(nextPage) {
    const nextParams = {};
    if (status) nextParams.status = status;
    if (merchantId.trim()) nextParams.merchantId = merchantId.trim();
    nextParams.page = String(nextPage);
    nextParams.size = String(size || DEFAULT_SIZE);

    setSearchParams(nextParams);
  }

  function handleRowClick(settlementId) {
    if (!settlementId) return;
    navigate(`/admin/settlements/${settlementId}`);
  }

  if (authChecking) {
    return (
      <PageLayout
        title={pageTitle}
        description="정산 상태와 판매자 기준으로 정산을 조회하고, settlementId 앵커로 A4 상세 화면으로 이동합니다."
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
          description="정산 상태와 판매자 기준으로 정산을 조회하고, settlementId 앵커로 A4 상세 화면으로 이동합니다."
        >
          <RequireLoginNotice
            title="인증 필요"
            message={authErrorMessage}
          />
        </PageLayout>
      );
    }

    return (
      <PageLayout
        title={pageTitle}
        description="정산 상태와 판매자 기준으로 정산을 조회하고, settlementId 앵커로 A4 상세 화면으로 이동합니다."
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
      description="정산 상태와 판매자 기준으로 정산을 조회하고, settlementId 앵커로 A4 상세 화면으로 이동합니다."
    >
      <SectionCard title="검색 조건">
        <form onSubmit={handleSearch}>
          <div className="filter-bar">
            <div className="form-field">
              <label className="form-field__label" htmlFor="status">
                status
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

            <div className="form-field">
              <label className="form-field__label" htmlFor="merchantId">
                merchantId
              </label>
              <input
                id="merchantId"
                className="input"
                type="text"
                value={merchantId}
                onChange={(event) => setMerchantId(event.target.value)}
                placeholder="merchantId 입력"
              />
            </div>
          </div>

          <div className="button-row action-panel" style={{ marginTop: "16px" }}>
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

            <Link to="/admin/settlement-batches" className="btn btn--secondary">
              배치 콘솔(A2)
            </Link>
          </div>
        </form>
      </SectionCard>

      {errorMessage ? (
        <>
          <GuardNotice title="조회 실패" message={errorMessage} tone="danger" />
          <ErrorState message={errorMessage} />
        </>
      ) : null}

      <SectionCard title="목록">
        <div className="table-toolbar">
          <div>
            <div>정산 관리 목록</div>
            <div
              style={{
                marginTop: "4px",
                color: "var(--color-text-muted, #6b7280)",
                fontSize: "14px",
              }}
            >
              status / merchantId 기준으로 정산을 조회하고 settlementId 앵커로 A4 상세 화면으로 이동합니다.
            </div>
          </div>

          <div
            style={{
              color: "var(--color-text-muted, #6b7280)",
              fontSize: "14px",
            }}
          >
            총 {totalElements}건
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
            description="검색 조건을 다시 확인하거나 필터를 초기화해 주세요."
          />
        ) : (
          <>
            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>settlementId</th>
                    <th>status</th>
                    <th>baseDate</th>
                    <th>merchantId</th>
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
                    const rowMerchantId = row?.merchantId ?? "-";

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

                        <td
                          onClick={(event) => event.stopPropagation()}
                          style={{ verticalAlign: "middle" }}
                        >
                          <CopyableId value={rowMerchantId} short />
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

            <div className="pagination">
              <button
                type="button"
                className="btn btn--secondary"
                onClick={() => handlePageChange(page - 1)}
                disabled={loading || !hasPrevious || page <= 0}
              >
                이전
              </button>

              <button type="button" className="btn btn--primary" disabled>
                {page + 1}
              </button>

              <button
                type="button"
                className="btn btn--secondary"
                onClick={() => handlePageChange(page + 1)}
                disabled={loading || !hasNext || page + 1 >= totalPages}
              >
                다음
              </button>
            </div>
          </>
        )}
      </SectionCard>
    </PageLayout>
  );
}