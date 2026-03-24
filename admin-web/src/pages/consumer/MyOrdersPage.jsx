// admin-web/src/pages/consumer/MyOrdersPage.jsx

import { useEffect, useMemo, useState } from "react";
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
import RequireLoginNotice from "../../components/feedback/RequireLoginNotice.jsx";

import { formatDateTime } from "../../utils/format.js";
import { getMe } from "../../api/meApi.js";
import { getConsumerOrders } from "../../api/consumerOrderApi.js";

const PAGE_TITLE = "내 주문/결제 내역";
const PAGE_DESCRIPTION =
  "C3 내 주문/결제 내역. row 클릭 시 주문 상세 화면으로 이동합니다.";

const STATUS_OPTIONS = [
  { value: "", label: "전체" },
  { value: "CREATED", label: "CREATED" },
  { value: "PAID", label: "PAID" },
];

function parseStatus(searchParams) {
  return searchParams.get("status") ?? "";
}

function isConsumerRole(me) {
  if (!me) return false;

  if (me.role === "CONSUMER" || me.role === "ROLE_CONSUMER") {
    return true;
  }

  if (
    Array.isArray(me.authorities) &&
    me.authorities.includes("ROLE_CONSUMER")
  ) {
    return true;
  }

  return false;
}

function buildErrorInfo(error, fallbackMessage) {
  return {
    status: error?.status ?? null,
    message:
      error?.body?.message ||
      error?.body?.reason ||
      error?.message ||
      fallbackMessage,
  };
}

function normalizeOrderList(payload) {
  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload?.items)) return payload.items;
  if (Array.isArray(payload?.data)) return payload.data;
  if (Array.isArray(payload?.content)) return payload.content;
  return [];
}

function formatAmount(value) {
  if (value === null || value === undefined || value === "") {
    return "-";
  }

  const numberValue = Number(value);
  if (Number.isNaN(numberValue)) {
    return String(value);
  }

  return `${numberValue.toLocaleString("ko-KR")}원`;
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

function getAmount(row) {
  return row?.amount ?? row?.totalAmount ?? row?.paymentAmount ?? null;
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

function isConfirmed(row) {
  return (
    row?.confirmed === true ||
    row?.isConfirmed === true ||
    Boolean(row?.confirmedAt) ||
    Boolean(row?.paymentConfirmedAt)
  );
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

  const queryStatus = useMemo(() => parseStatus(searchParams), [searchParams]);

  const [status, setStatus] = useState(queryStatus);

  const [me, setMe] = useState(null);
  const [meLoading, setMeLoading] = useState(true);
  const [meErrorInfo, setMeErrorInfo] = useState(null);

  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(false);
  const [errorInfo, setErrorInfo] = useState(null);

  useEffect(() => {
    setStatus(queryStatus);
  }, [queryStatus]);

  useEffect(() => {
    let cancelled = false;

    async function loadMe() {
      try {
        setMeLoading(true);
        setMeErrorInfo(null);

        const meData = await getMe();

        if (cancelled) return;

        setMe(meData);

        if (!isConsumerRole(meData)) {
          setMeErrorInfo({
            status: 403,
            message:
              "Consumer 권한이 필요한 페이지입니다. /auth/dev-login 에서 Consumer 세션으로 다시 로그인해 주세요.",
          });
        }
      } catch (error) {
        if (cancelled) return;

        setMe(null);
        setMeErrorInfo(
          buildErrorInfo(
            error,
            "현재 세션 정보를 불러오지 못했습니다. /auth/dev-login 에서 다시 로그인해 주세요."
          )
        );
      } finally {
        if (!cancelled) {
          setMeLoading(false);
        }
      }
    }

    loadMe();

    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (meLoading || meErrorInfo || !isConsumerRole(me)) {
      return;
    }

    let cancelled = false;

    async function loadOrders() {
      try {
        setLoading(true);
        setErrorInfo(null);

        const params = {};
        if (queryStatus) {
          params.status = queryStatus;
        }

        const data = await getConsumerOrders(params);

        if (cancelled) return;

        setRows(normalizeOrderList(data));
      } catch (error) {
        if (cancelled) return;

        setRows([]);
        setErrorInfo(
          buildErrorInfo(
            error,
            "주문/결제 내역을 불러오지 못했습니다. 백엔드 연결 상태와 응답 구조를 확인해 주세요."
          )
        );
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    loadOrders();

    return () => {
      cancelled = true;
    };
  }, [meLoading, meErrorInfo, me, queryStatus]);

  function handleSearch(event) {
    event.preventDefault();

    const nextParams = {};
    if (status) {
      nextParams.status = status;
    }

    setSearchParams(nextParams);
  }

  function handleReset() {
    setStatus("");
    setSearchParams({});
  }

  function handleRefresh() {
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev);
      return next;
    });
  }

  function handleRowClick(orderId) {
    if (!orderId) return;
    navigate(`/consumer/orders/${orderId}`);
  }

  if (meLoading) {
    return (
      <PageLayout title={PAGE_TITLE} description={PAGE_DESCRIPTION}>
        <SectionCard title="세션 확인 중">
          <LoadingBlock
            title="세션 확인 중"
            description="현재 로그인 세션의 역할을 확인하고 있습니다."
          />
        </SectionCard>
      </PageLayout>
    );
  }

  if (meErrorInfo?.status === 401) {
    return (
      <PageLayout title={PAGE_TITLE} description={PAGE_DESCRIPTION}>
        <RequireLoginNotice
          title="Consumer 전용 화면"
          message={meErrorInfo.message}
        />
      </PageLayout>
    );
  }

  if (meErrorInfo?.status === 403) {
    return (
      <PageLayout title={PAGE_TITLE} description={PAGE_DESCRIPTION}>
        <GuardNotice
          title="접근 불가"
          message={meErrorInfo.message}
          tone="danger"
        />
      </PageLayout>
    );
  }

  if (meErrorInfo) {
    return (
      <PageLayout title={PAGE_TITLE} description={PAGE_DESCRIPTION}>
        <GuardNotice
          title="세션 확인 실패"
          message={meErrorInfo.message}
          tone="danger"
        />
      </PageLayout>
    );
  }

  return (
    <PageLayout title={PAGE_TITLE} description={PAGE_DESCRIPTION}>
      <SectionCard title="조회 조건">
        <form onSubmit={handleSearch}>
          <div className="filter-bar">
            <div className="form-field">
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

            <div className="button-row action-panel">
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

      {errorInfo ? (
        <>
          <GuardNotice
            title="조회 실패"
            message={errorInfo.message}
            tone="danger"
          />
          <ErrorState message={errorInfo.message} />
        </>
      ) : null}

      <SectionCard title="주문/결제 목록">
        <div
          className="table-toolbar"
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            width: "100%",
            marginBottom: "16px",
          }}
        >
          <div>내 주문/결제 내역</div>

          <div
            className="action-panel"
            style={{
              marginLeft: "auto",
              display: "flex",
              justifyContent: "flex-end",
            }}
          >
            <ActionButton
              type="button"
              variant="secondary"
              onClick={handleRefresh}
              disabled={loading}
            >
              새로고침
            </ActionButton>
          </div>
        </div>

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
                  <th>amount</th>
                  <th>order status</th>
                  <th>payment status</th>
                  <th>confirmed</th>
                  <th>createdAt</th>
                  <th>상세</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row, index) => {
                  const orderId = getOrderId(row, index);
                  const paymentId = getPaymentId(row);
                  const confirmed = isConfirmed(row);
                  const paymentStatus = getPaymentStatus(row);

                  return (
                    <tr
                      key={orderId}
                      onClick={() => handleRowClick(orderId)}
                      style={{ cursor: orderId ? "pointer" : "default" }}
                    >
                      <td onClick={(event) => event.stopPropagation()}>
                        <CopyableCell value={orderId} />
                      </td>

                      <td onClick={(event) => event.stopPropagation()}>
                        <CopyableCell value={paymentId || "-"} />
                      </td>

                      <td>{getItemName(row)}</td>
                      <td>{formatAmount(getAmount(row))}</td>

                      <td>
                        <StatusBadge status={getOrderStatus(row)} />
                      </td>

                      <td>
                        {paymentStatus === "-" ? (
                          "-"
                        ) : (
                          <StatusBadge status={paymentStatus} />
                        )}
                      </td>

                      <td>
                        <StatusBadge
                          status={confirmed ? "CONFIRMED" : "UNCONFIRMED"}
                        />
                      </td>

                      <td>{formatDateTime(getCreatedAt(row))}</td>

                      <td onClick={(event) => event.stopPropagation()}>
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

      <SectionCard title="페이지 규칙">
        <div className="info-list">
          <div>
            <strong>역할</strong> Consumer
          </div>
          <div>
            <strong>핵심 이동</strong> C3 row 클릭 → 주문 상세(orderId 전달)
          </div>
          <div>
            <strong>주문 상태</strong> order.status는 CREATED / PAID 사용
          </div>
          <div>
            <strong>결제 상태</strong> payment.status는 별도 컬럼으로 표시
          </div>
          <div>
            <strong>확정 여부</strong> PAYMENT_CONFIRMED 이벤트 존재 여부 기반 파생 표시
          </div>
          <div>
            <strong>검색 기준</strong> status
          </div>
          <div>
            <strong>로그인 주체</strong> {me?.buyerId ?? me?.userId ?? "-"}
          </div>
        </div>
      </SectionCard>
    </PageLayout>
  );
}