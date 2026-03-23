// /admin-web/src/pages/merchant/PaymentListPage.jsx

import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { formatDateTime, formatNumber } from "../../utils/format.js";
import { getMe } from "../../api/meApi.js";
import { getMerchantPayments } from "../../api/merchantPaymentApi.js";
import PageLayout from "../../components/layout/PageLayout.jsx";
import SectionCard from "../../components/layout/SectionCard.jsx";
import StatusBadge from "../../components/display/StatusBadge.jsx";
import RequireLoginNotice from "../../components/feedback/RequireLoginNotice.jsx";
import Pagination from "../../components/table/Pagination.jsx";
import usePagination from "../../hooks/usePagination.js";

const PAGE_TITLE = "결제 조회";
const PAGE_DESCRIPTION =
  "U2 결제 조회/검색. row 클릭 시 U3 결제 상세로 이동하는 리스트형 페이지입니다.";

function isMerchantRole(me) {
  if (!me) {
    return false;
  }

  if (me.role === "MERCHANT" || me.role === "ROLE_MERCHANT") {
    return true;
  }

  if (Array.isArray(me.authorities) && me.authorities.includes("ROLE_MERCHANT")) {
    return true;
  }

  return false;
}

function buildErrorInfo(error, fallbackMessage) {
  return {
    status: error?.status ?? null,
    message: error?.body?.message || error?.body?.reason || fallbackMessage,
  };
}

export default function PaymentListPage() {
  const navigate = useNavigate();
  const { page, size, setPage, setSize, resetPage } = usePagination(0, 20);

  const [me, setMe] = useState(null);
  const [filters, setFilters] = useState({
    status: "ALL",
    from: "",
    to: "",
    keyword: "",
  });

  const [payments, setPayments] = useState([]);
  const [pageInfo, setPageInfo] = useState({
    page: 0,
    size: 20,
    totalElements: 0,
    totalPages: 0,
    hasNext: false,
    hasPrevious: false,
  });

  const [loading, setLoading] = useState(true);
  const [errorInfo, setErrorInfo] = useState(null);

  function applyPageResponse(response, fallbackSize = size) {
    setPayments(response?.items ?? []);
    setPageInfo({
      page: response?.page ?? 0,
      size: response?.size ?? fallbackSize,
      totalElements: response?.totalElements ?? 0,
      totalPages: response?.totalPages ?? 0,
      hasNext: response?.hasNext ?? false,
      hasPrevious: response?.hasPrevious ?? false,
    });
  }

  function clearPageResponse(nextSize = size) {
    setPayments([]);
    setPageInfo({
      page: 0,
      size: nextSize,
      totalElements: 0,
      totalPages: 0,
      hasNext: false,
      hasPrevious: false,
    });
  }

  async function loadPayments(
    nextFilters = filters,
    merchantIdParam = me?.merchantId,
    nextPage = page,
    nextSize = size
  ) {
    if (!merchantIdParam) {
      clearPageResponse(nextSize);
      return;
    }

    const response = await getMerchantPayments({
      merchantId: merchantIdParam,
      status: nextFilters.status,
      from: nextFilters.from,
      to: nextFilters.to,
      keyword: nextFilters.keyword,
      page: nextPage,
      size: nextSize,
    });

    applyPageResponse(response, nextSize);
  }

  async function runPaymentsLoad(
    nextFilters = filters,
    nextPage = page,
    nextSize = size,
    merchantIdParam = me?.merchantId
  ) {
    try {
      setLoading(true);
      setErrorInfo(null);
      await loadPayments(nextFilters, merchantIdParam, nextPage, nextSize);
    } catch (error) {
      clearPageResponse(nextSize);
      setErrorInfo(buildErrorInfo(error, "결제 목록을 불러오지 못했습니다."));
    } finally {
      setLoading(false);
    }
  }

  async function loadPage() {
    try {
      setLoading(true);
      setErrorInfo(null);

      const meData = await getMe();
      setMe(meData);

      if (!isMerchantRole(meData)) {
        clearPageResponse(size);
        setErrorInfo({
          status: 403,
          message: "Merchant 권한이 필요한 페이지입니다.",
        });
        return;
      }

      await loadPayments(filters, meData?.merchantId, page, size);
    } catch (error) {
      setMe(null);
      clearPageResponse(size);
      setErrorInfo(buildErrorInfo(error, "페이지 정보를 불러오지 못했습니다."));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadPage();
  }, []);

  function handleRowClick(paymentId) {
    navigate(`/merchant/payments/${paymentId}`);
  }

  function handleRowKeyDown(event, paymentId) {
    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      navigate(`/merchant/payments/${paymentId}`);
    }
  }

  function handleFilterChange(event) {
    const { name, value } = event.target;
    setFilters((prev) => ({
      ...prev,
      [name]: value,
    }));
  }

  async function handleSearch(event) {
    event?.preventDefault?.();

    if (page !== 0) {
      resetPage();
      await runPaymentsLoad(filters, 0, size, me?.merchantId);
      return;
    }

    await runPaymentsLoad(filters, 0, size, me?.merchantId);
  }

  async function handleReset() {
    const initialFilters = {
      status: "ALL",
      from: "",
      to: "",
      keyword: "",
    };

    setFilters(initialFilters);

    if (page !== 0) {
      resetPage();
      await runPaymentsLoad(initialFilters, 0, size, me?.merchantId);
      return;
    }

    await runPaymentsLoad(initialFilters, 0, size, me?.merchantId);
  }

  async function handleRefresh() {
    await runPaymentsLoad(filters, page, size, me?.merchantId);
  }

  async function handlePageChange(nextPage) {
    setPage(nextPage);
    await runPaymentsLoad(filters, nextPage, size, me?.merchantId);
  }

  async function handleSizeChange(event) {
    const nextSize = Number(event.target.value);
    setSize(nextSize);

    if (page !== 0) {
      resetPage();
      await runPaymentsLoad(filters, 0, nextSize, me?.merchantId);
      return;
    }

    await runPaymentsLoad(filters, 0, nextSize, me?.merchantId);
  }

  if (loading) {
    return (
      <PageLayout title={PAGE_TITLE} description={PAGE_DESCRIPTION}>
        <div className="guard-notice">
          <div className="guard-notice__title">로딩 중</div>
          <div className="guard-notice__description">
            merchant 결제 목록을 불러오는 중입니다.
          </div>
        </div>
      </PageLayout>
    );
  }

  if (errorInfo?.status === 401) {
    return (
      <PageLayout title={PAGE_TITLE} description={PAGE_DESCRIPTION}>
        <RequireLoginNotice />
      </PageLayout>
    );
  }

  if (errorInfo?.status === 403) {
    return (
      <PageLayout title={PAGE_TITLE} description={PAGE_DESCRIPTION}>
        <div className="guard-notice">
          <div className="guard-notice__title">접근 불가</div>
          <div className="guard-notice__description">{errorInfo.message}</div>
        </div>
      </PageLayout>
    );
  }

  if (errorInfo) {
    return (
      <PageLayout title={PAGE_TITLE} description={PAGE_DESCRIPTION}>
        <div className="guard-notice">
          <div className="guard-notice__title">조회 실패</div>
          <div className="guard-notice__description">{errorInfo.message}</div>
        </div>
      </PageLayout>
    );
  }

  return (
    <PageLayout title={PAGE_TITLE} description={PAGE_DESCRIPTION}>
      <SectionCard title="검색 조건">
        <div className="filter-bar">
          <div className="form-field">
            <label className="form-field__label">status</label>
            <select
              className="select"
              name="status"
              value={filters.status}
              onChange={handleFilterChange}
            >
              <option value="ALL">전체</option>
              <option value="CREATED">CREATED</option>
              <option value="CAPTURED">CAPTURED</option>
            </select>
          </div>

          <div className="form-field">
            <label className="form-field__label">from</label>
            <input
              className="input"
              type="date"
              name="from"
              value={filters.from}
              onChange={handleFilterChange}
            />
          </div>

          <div className="form-field">
            <label className="form-field__label">to</label>
            <input
              className="input"
              type="date"
              name="to"
              value={filters.to}
              onChange={handleFilterChange}
            />
          </div>

          <div className="form-field search-field">
            <label className="form-field__label">keyword</label>
            <input
              className="input"
              name="keyword"
              value={filters.keyword}
              onChange={handleFilterChange}
              placeholder="paymentId / orderId / itemName"
            />
          </div>
        </div>

        <div className="button-row action-panel" style={{ marginTop: "16px" }}>
          <button type="button" className="btn btn--primary" onClick={handleSearch}>
            조회
          </button>
          <button type="button" className="btn btn--secondary" onClick={handleReset}>
            초기화
          </button>
        </div>
      </SectionCard>

      <SectionCard title="결제 목록">
        <div className="table-toolbar">
          <div>merchant 결제 목록 · 총 {formatNumber(pageInfo.totalElements)}건</div>

          <div className="action-panel">
            <select className="select" value={size} onChange={handleSizeChange}>
              <option value={10}>10개</option>
              <option value={20}>20개</option>
              <option value={50}>50개</option>
            </select>

            <button type="button" className="btn btn--secondary" onClick={handleRefresh}>
              새로고침
            </button>
          </div>
        </div>

        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>paymentId</th>
                <th>orderId</th>
                <th>itemName</th>
                <th>buyerId</th>
                <th>status</th>
                <th>confirmed</th>
                <th>requestedAmount</th>
                <th>capturedAmount</th>
                <th>capturedAt</th>
                <th>confirmedAt</th>
              </tr>
            </thead>
            <tbody>
              {payments.length === 0 ? (
                <tr>
                  <td colSpan={10}>
                    <div className="empty-state">조회 결과가 없습니다.</div>
                  </td>
                </tr>
              ) : (
                payments.map((payment) => (
                  <tr
                    key={payment.paymentId}
                    style={{ cursor: "pointer" }}
                    onClick={() => handleRowClick(payment.paymentId)}
                    onKeyDown={(e) => handleRowKeyDown(e, payment.paymentId)}
                    tabIndex={0}
                    role="button"
                    className="clickable-row"
                  >
                    <td>
                      <Link
                        to={`/merchant/payments/${payment.paymentId}`}
                        onClick={(e) => e.stopPropagation()}
                      >
                        {payment.paymentId}
                      </Link>
                    </td>
                    <td>{payment.orderId}</td>
                    <td>{payment.itemName}</td>
                    <td>{payment.buyerId}</td>
                    <td>
                      <StatusBadge status={payment.status} />
                    </td>
                    <td>
                      {payment.confirmed ? (
                        <span className="status-text status-text--done">CONFIRMED</span>
                      ) : (
                        <span className="status-text">-</span>
                      )}
                    </td>
                    <td>{formatNumber(payment.requestedAmount)}</td>
                    <td>{formatNumber(payment.capturedAmount)}</td>
                    <td>{formatDateTime(payment.capturedAt)}</td>
                    <td>{formatDateTime(payment.confirmedAt)}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        <Pagination
          page={pageInfo.page}
          totalPages={pageInfo.totalPages}
          onPageChange={handlePageChange}
          disabled={loading}
        />
      </SectionCard>

      <SectionCard title="페이지 규칙">
        <div className="info-list">
          <div><strong>역할</strong> Merchant</div>
          <div><strong>핵심 이동</strong> U2 row 클릭 → U3 결제 상세(paymentId 전달)</div>
          <div><strong>표시 기준</strong> payment.status는 CREATED / CAPTURED만 사용</div>
          <div><strong>확정 여부</strong> confirmed / confirmedAt 파생값으로 표시</div>
          <div><strong>검색 기준</strong> status + from + to + keyword</div>
          <div><strong>페이지 정보</strong> {pageInfo.page + 1} / {Math.max(pageInfo.totalPages, 1)}</div>
          <div><strong>로그인 주체</strong> {me?.merchantId ?? "-"}</div>
        </div>
      </SectionCard>
    </PageLayout>
  );
}