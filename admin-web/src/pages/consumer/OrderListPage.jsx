// /admin-web/src/pages/consumer/OrderListPage.jsx

import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { getConsumerOrders } from "../../api/consumerOrderApi.js";
import { formatDateTime, formatKrw } from "../../util/format.js";
import PageLayout from "../../components/layout/PageLayout.jsx";
import SectionCard from "../../components/layout/SectionCard.jsx";
import StatusBadge from "../../components/display/StatusBadge.jsx";
import RequireLoginNotice from "../../components/common/RequireLoginNotice.jsx";

export default function OrderListPage() {
  const navigate = useNavigate();

  const [filters, setFilters] = useState({
    status: "ALL",
    confirmed: "ALL",
    keyword: "",
  });

  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [errorInfo, setErrorInfo] = useState(null);

  async function loadOrders(nextFilters = filters) {
    const data = await getConsumerOrders({
      status: nextFilters.status,
      confirmed: nextFilters.confirmed,
      keyword: nextFilters.keyword,
    });

    setOrders(Array.isArray(data) ? data : []);
  }

  async function loadPage() {
    try {
      setLoading(true);
      setErrorInfo(null);
      await loadOrders(filters);
    } catch (error) {
      setOrders([]);
      setErrorInfo({
        status: error?.status ?? null,
        message:
          error?.body?.message ||
          error?.body?.reason ||
          "주문 / 결제 내역을 불러오지 못했습니다.",
      });
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadPage();
  }, []);

  function handleRowClick(orderId) {
    navigate(`/consumer/orders/${orderId}`);
  }

  function handleRowKeyDown(event, orderId) {
    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      navigate(`/consumer/orders/${orderId}`);
    }
  }

  function handleFilterChange(event) {
    const { name, value } = event.target;
    setFilters((prev) => ({
      ...prev,
      [name]: value,
    }));
  }

  async function handleSearch() {
    try {
      setLoading(true);
      setErrorInfo(null);
      await loadOrders(filters);
    } catch (error) {
      setOrders([]);
      setErrorInfo({
        status: error?.status ?? null,
        message:
          error?.body?.message ||
          error?.body?.reason ||
          "주문 / 결제 내역을 불러오지 못했습니다.",
      });
    } finally {
      setLoading(false);
    }
  }

  async function handleReset() {
    const initialFilters = {
      status: "ALL",
      confirmed: "ALL",
      keyword: "",
    };

    try {
      setFilters(initialFilters);
      setLoading(true);
      setErrorInfo(null);
      await loadOrders(initialFilters);
    } catch (error) {
      setOrders([]);
      setErrorInfo({
        status: error?.status ?? null,
        message:
          error?.body?.message ||
          error?.body?.reason ||
          "주문 / 결제 내역을 불러오지 못했습니다.",
      });
    } finally {
      setLoading(false);
    }
  }

  async function handleRefresh() {
    try {
      setLoading(true);
      setErrorInfo(null);
      await loadOrders(filters);
    } catch (error) {
      setOrders([]);
      setErrorInfo({
        status: error?.status ?? null,
        message:
          error?.body?.message ||
          error?.body?.reason ||
          "주문 / 결제 내역을 불러오지 못했습니다.",
      });
    } finally {
      setLoading(false);
    }
  }

  if (loading) {
    return (
      <PageLayout
        title="내 주문/결제 내역"
        description="C3 내 주문/결제 내역. row 클릭 시 C2 결제 상세로 이동하는 리스트형 페이지입니다."
      >
        <div className="guard-notice">
          <div className="guard-notice__title">로딩 중</div>
          <div className="guard-notice__description">
            주문 / 결제 내역을 불러오는 중입니다.
          </div>
        </div>
      </PageLayout>
    );
  }

  if (errorInfo?.status === 401) {
    return (
      <PageLayout
        title="내 주문/결제 내역"
        description="C3 내 주문/결제 내역. row 클릭 시 C2 결제 상세로 이동하는 리스트형 페이지입니다."
      >
        <RequireLoginNotice />
      </PageLayout>
    );
  }

  if (errorInfo?.status === 403) {
    return (
      <PageLayout
        title="내 주문/결제 내역"
        description="C3 내 주문/결제 내역. row 클릭 시 C2 결제 상세로 이동하는 리스트형 페이지입니다."
      >
        <div className="guard-notice">
          <div className="guard-notice__title">접근 불가</div>
          <div className="guard-notice__description">
            {errorInfo.message}
          </div>
        </div>
      </PageLayout>
    );
  }

  if (errorInfo) {
    return (
      <PageLayout
        title="내 주문/결제 내역"
        description="C3 내 주문/결제 내역. row 클릭 시 C2 결제 상세로 이동하는 리스트형 페이지입니다."
      >
        <div className="guard-notice">
          <div className="guard-notice__title">조회 실패</div>
          <div className="guard-notice__description">
            {errorInfo.message}
          </div>
        </div>
      </PageLayout>
    );
  }

  return (
    <PageLayout
      title="내 주문/결제 내역"
      description="C3 내 주문/결제 내역. row 클릭 시 C2 결제 상세로 이동하는 리스트형 페이지입니다."
    >
      <SectionCard title="검색 조건">
        <div className="filter-bar">
          <div className="form-field">
            <label className="form-field__label">orderStatus</label>
            <select
              className="select"
              name="status"
              value={filters.status}
              onChange={handleFilterChange}
            >
              <option value="ALL">전체</option>
              <option value="CREATED">CREATED</option>
              <option value="PAID">PAID</option>
            </select>
          </div>

          <div className="form-field">
            <label className="form-field__label">confirmed</label>
            <select
              className="select"
              name="confirmed"
              value={filters.confirmed}
              onChange={handleFilterChange}
            >
              <option value="ALL">전체</option>
              <option value="CONFIRMED">CONFIRMED</option>
              <option value="UNCONFIRMED">UNCONFIRMED</option>
            </select>
          </div>

          <div className="form-field search-field">
            <label className="form-field__label">keyword</label>
            <input
              className="input"
              name="keyword"
              value={filters.keyword}
              onChange={handleFilterChange}
              placeholder="orderId / paymentId / itemName"
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

      <SectionCard title="주문 / 결제 목록">
        <div className="table-toolbar">
          <div>내 주문/결제 내역</div>
          <div className="action-panel">
            <button type="button" className="btn btn--secondary" onClick={handleRefresh}>
              새로고침
            </button>
          </div>
        </div>

        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>orderId</th>
                <th>paymentId</th>
                <th>itemName</th>
                <th>amount</th>
                <th>orderStatus</th>
                <th>paymentStatus</th>
                <th>confirmed</th>
                <th>confirmedAt</th>
              </tr>
            </thead>
            <tbody>
              {orders.length === 0 ? (
                <tr>
                  <td colSpan={8}>
                    <div className="empty-state">조회 결과가 없습니다.</div>
                  </td>
                </tr>
              ) : (
                orders.map((order) => (
                  <tr
                    key={order.orderId}
                    onClick={() => handleRowClick(order.orderId)}
                    onKeyDown={(e) => handleRowKeyDown(e, order.orderId)}
                    tabIndex={0}
                    role="button"
                    className="clickable-row"
                  >
                    <td>
                      <Link
                        to={`/consumer/orders/${order.orderId}`}
                        onClick={(e) => e.stopPropagation()}
                      >
                        {order.orderId}
                      </Link>
                    </td>
                    <td>{order.paymentId ?? "-"}</td>
                    <td>{order.itemName}</td>
                    <td>{formatKrw(order.amount)}</td>
                    <td>
                      <StatusBadge status={order.orderStatus} />
                    </td>
                    <td>
                      {order.paymentStatus ? (
                        <StatusBadge status={order.paymentStatus} />
                      ) : (
                        "-"
                      )}
                    </td>
                    <td>
                      {order.confirmed ? (
                        <span className="status-text status-text--done">CONFIRMED</span>
                      ) : (
                        <span className="status-text">-</span>
                      )}
                    </td>
                    <td>{formatDateTime(order.confirmedAt)}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </SectionCard>

      <SectionCard title="페이지 규칙">
        <div className="info-list">
          <div><strong>역할</strong> Consumer</div>
          <div><strong>핵심 이동</strong> C3 row 클릭 → C2 결제 상세(orderId 전달)</div>
          <div><strong>주문 상태</strong> order.status는 CREATED / PAID 사용</div>
          <div><strong>결제 상태</strong> payment.status는 CREATED / CAPTURED 사용</div>
          <div><strong>확정 여부</strong> confirmed / confirmedAt 파생값으로 표시</div>
        </div>
      </SectionCard>
    </PageLayout>
  );
}