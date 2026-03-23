import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";

import PageLayout from "../../components/layout/PageLayout.jsx";
import SectionCard from "../../components/layout/SectionCard.jsx";
import ActionButton from "../../components/layout/ActionButton.jsx";
import StatusBadge from "../../components/display/StatusBadge.jsx";
import CopyableId from "../../components/display/CopyableId.jsx";
import GuardNotice from "../../components/common/GuardNotice.jsx";
import EmptyState from "../../components/feedback/EmptyState.jsx";
import ErrorState from "../../components/feedback/ErrorState.jsx";
import LoadingBlock from "../../components/feedback/LoadingBlock.jsx";
import { formatDateTime } from "../../utils/format.js";
import { getConsumerOrders } from "../../api/consumerOrderApi.js";

const STATUS_OPTIONS = [
  { value: "", label: "전체" },
  { value: "CREATED", label: "CREATED" },
  { value: "PAID", label: "PAID" },
];

function formatAmount(value) {
  if (value === null || value === undefined || value === "") return "-";
  const num = Number(value);
  if (Number.isNaN(num)) return String(value);
  return `${num.toLocaleString("ko-KR")}원`;
}

function normalizeOrderList(payload) {
  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload?.data)) return payload.data;
  if (Array.isArray(payload?.items)) return payload.items;
  if (Array.isArray(payload?.content)) return payload.content;
  return [];
}

function buildErrorMessage(error) {
  const status = error?.status;
  const message =
    error?.body?.message ||
    error?.body?.reason ||
    error?.message;

  if (status === 401) {
    return "인증이 만료되었거나 로그인되지 않았습니다. /auth/dev-login 에서 Consumer 세션을 다시 생성해 주세요.";
  }

  if (status === 403) {
    return "Consumer 권한이 없어 주문/결제 내역을 조회할 수 없습니다.";
  }

  if (status === 404) {
    return "주문/결제 내역 API 경로를 찾을 수 없습니다. 백엔드 라우팅을 확인해 주세요.";
  }

  return (
    message ||
    "주문/결제 내역을 불러오지 못했습니다. 백엔드 연결 상태와 API 응답 구조를 확인해 주세요."
  );
}

function isConfirmed(row) {
  return (
    row?.confirmed === true ||
    row?.isConfirmed === true ||
    Boolean(row?.confirmedAt) ||
    Boolean(row?.paymentConfirmedAt)
  );
}

function getOrderId(row, index) {
  return row?.orderId ?? row?.id ?? `row-${index}`;
}

function getPaymentId(row) {
  return row?.paymentId ?? row?.payment?.paymentId ?? null;
}

function getItemName(row) {
  return row?.itemName ?? row?.productName ?? row?.title ?? "-";
}

function getCreatedAt(row) {
  return row?.createdAt ?? row?.orderedAt ?? row?.orderCreatedAt ?? null;
}

function getOrderStatus(row) {
  return row?.orderStatus ?? row?.status ?? "UNKNOWN";
}

function getPaymentStatus(row) {
  return row?.paymentStatus ?? row?.payment?.status ?? "-";
}

function CopyableCell({ value }) {
  if (!value || value === "-") {
    return <span>-</span>;
  }

  return <CopyableId value={value} short />;
}

export default function MyOrdersPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  const [status, setStatus] = useState(searchParams.get("status") ?? "");

  const pageTitle = useMemo(() => "내 주문/결제 내역", []);

  const fetchOrders = useCallback(async (nextStatus = "") => {
    setLoading(true);
    setErrorMessage("");

    try {
      const params = {};
      if (nextStatus) params.status = nextStatus;

      const data = await getConsumerOrders(params);
      setRows(normalizeOrderList(data));
    } catch (error) {
      console.error("C3 주문/결제 내역 조회 실패", error);
      setRows([]);
      setErrorMessage(buildErrorMessage(error));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const nextStatus = searchParams.get("status") ?? "";
    setStatus(nextStatus);
    fetchOrders(nextStatus);
  }, [searchParams, fetchOrders]);

  const handleSearch = (event) => {
    event.preventDefault();

    const nextParams = {};
    if (status) nextParams.status = status;

    setSearchParams(nextParams);
  };

  const handleReset = () => {
    setStatus("");
    setSearchParams({});
  };

  const handleRowClick = (orderId) => {
    if (!orderId) return;
    navigate(`/consumer/orders/${orderId}`);
  };

  return (
    <PageLayout
      title={pageTitle}
      description="Consumer 기준으로 주문/결제 내역을 조회하고, pay 이후 PAID와 CONFIRMED 상태를 함께 확인합니다."
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
                주문 상태
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
              className="button-row action-panel"
              style={{
                display: "flex",
                alignItems: "center",
                gap: "12px",
                flexWrap: "wrap",
              }}
            >
              <ActionButton type="submit" variant="primary" disabled={loading}>
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
            </div>
          </div>
        </form>
      </SectionCard>

      <GuardNotice
        title="상태 해석 안내"
        tone="info"
        message="order.status는 CREATED / PAID 기준으로 보이며, CONFIRMED는 payment_event 파생 상태로 별도 표시합니다."
      />

      {errorMessage ? (
        <>
          <GuardNotice title="조회 실패" message={errorMessage} tone="danger" />
          <ErrorState
            title="주문/결제 내역 조회 실패"
            description={errorMessage}
          />
        </>
      ) : null}

      <SectionCard title="주문/결제 내역" description={`총 ${rows.length}건`}>
        {loading ? (
          <LoadingBlock
            title="로딩 중"
            description="주문/결제 내역을 불러오고 있습니다."
          />
        ) : rows.length === 0 ? (
          <EmptyState
            title="조회 결과가 없습니다"
            description="주문 상태를 바꾸거나 새 주문을 생성한 뒤 다시 확인해 주세요."
          />
        ) : (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>orderId</th>
                  <th>paymentId</th>
                  <th>itemName</th>
                  <th style={{ textAlign: "center" }}>amount</th>
                  <th style={{ textAlign: "center" }}>order status</th>
                  <th style={{ textAlign: "center" }}>payment status</th>
                  <th style={{ textAlign: "center" }}>confirmed</th>
                  <th style={{ textAlign: "center" }}>createdAt</th>
                  <th style={{ textAlign: "center" }}>상세</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row, index) => {
                  const orderId = getOrderId(row, index);
                  const paymentId = getPaymentId(row);
                  const confirmed = isConfirmed(row);

                  return (
                    <tr
                      key={orderId}
                      onClick={() => handleRowClick(orderId)}
                      style={{ cursor: orderId ? "pointer" : "default" }}
                    >
                      <td
                        onClick={(event) => event.stopPropagation()}
                        style={{ verticalAlign: "middle" }}
                      >
                        <CopyableCell value={orderId} />
                      </td>

                      <td
                        onClick={(event) => event.stopPropagation()}
                        style={{ verticalAlign: "middle" }}
                      >
                        <CopyableCell value={paymentId || "-"} />
                      </td>

                      <td style={{ verticalAlign: "middle" }}>
                        {getItemName(row)}
                      </td>

                      <td
                        style={{
                          verticalAlign: "middle",
                          textAlign: "center",
                        }}
                      >
                        {formatAmount(row?.amount)}
                      </td>

                      <td
                        style={{
                          verticalAlign: "middle",
                          textAlign: "center",
                        }}
                      >
                        <StatusBadge status={getOrderStatus(row)} />
                      </td>

                      <td
                        style={{
                          verticalAlign: "middle",
                          textAlign: "center",
                        }}
                      >
                        {getPaymentStatus(row) === "-" ? (
                          "-"
                        ) : (
                          <StatusBadge status={getPaymentStatus(row)} />
                        )}
                      </td>

                      <td
                        style={{
                          verticalAlign: "middle",
                          textAlign: "center",
                        }}
                      >
                        <StatusBadge
                          status={confirmed ? "CONFIRMED" : "UNCONFIRMED"}
                        />
                      </td>

                      <td
                        style={{
                          verticalAlign: "middle",
                          textAlign: "center",
                          whiteSpace: "nowrap",
                        }}
                      >
                        {formatDateTime(getCreatedAt(row))}
                      </td>

                      <td
                        onClick={(event) => event.stopPropagation()}
                        style={{
                          verticalAlign: "middle",
                          textAlign: "center",
                        }}
                      >
                        <ActionButton
                          type="button"
                          variant="secondary"
                          onClick={() => handleRowClick(orderId)}
                          disabled={!orderId}
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