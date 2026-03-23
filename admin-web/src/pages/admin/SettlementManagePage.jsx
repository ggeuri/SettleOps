import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate, Link, useSearchParams } from "react-router-dom";
import axios from "axios";

import PageLayout from "../../components/layout/PageLayout.jsx";
import SectionCard from "../../components/layout/SectionCard.jsx";
import ActionButton from "../../components/layout/ActionButton.jsx";
import StatusBadge from "../../components/display/StatusBadge.jsx";
import CopyableId from "../../components/display/CopyableId.jsx";
import GuardNotice from "../../components/common/GuardNotice.jsx";
import EmptyState from "../../components/feedback/EmptyState.jsx";
import ErrorState from "../../components/feedback/ErrorState.jsx";
import LoadingBlock from "../../components/feedback/LoadingBlock.jsx";
import { formatNumber } from "../../utils/format.js";

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

function normalizeSettlementList(payload) {
  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload?.data)) return payload.data;
  if (Array.isArray(payload?.items)) return payload.items;
  if (Array.isArray(payload?.content)) return payload.content;
  return [];
}

function buildErrorMessage(error) {
  const status = error?.response?.status;
  const message =
    error?.response?.data?.message ||
    error?.response?.data?.reason ||
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
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  const [status, setStatus] = useState(searchParams.get("status") ?? "");
  const [merchantId, setMerchantId] = useState(
    searchParams.get("merchantId") ?? ""
  );

  const pageTitle = useMemo(() => "A3 정산 관리", []);

  const fetchSettlements = useCallback(
    async (nextStatus = "", nextMerchantId = "") => {
      setLoading(true);
      setErrorMessage("");

      try {
        const params = {};
        if (nextStatus) params.status = nextStatus;
        if (nextMerchantId.trim()) params.merchantId = nextMerchantId.trim();

        const response = await axios.get("/api/admin/settlements", {
          withCredentials: true,
          params,
        });

        setRows(normalizeSettlementList(response.data));
      } catch (error) {
        console.error("정산 리스트 조회 실패", error);
        setRows([]);
        setErrorMessage(buildErrorMessage(error));
      } finally {
        setLoading(false);
      }
    },
    []
  );

  useEffect(() => {
    const nextStatus = searchParams.get("status") ?? "";
    const nextMerchantId = searchParams.get("merchantId") ?? "";

    setStatus(nextStatus);
    setMerchantId(nextMerchantId);
    fetchSettlements(nextStatus, nextMerchantId);
  }, [searchParams, fetchSettlements]);

  const handleSearch = (event) => {
    event.preventDefault();

    const nextParams = {};
    if (status) nextParams.status = status;
    if (merchantId.trim()) nextParams.merchantId = merchantId.trim();

    setSearchParams(nextParams);
  };

  const handleReset = () => {
    setStatus("");
    setMerchantId("");
    setSearchParams({});
  };

  const handleRowClick = (settlementId) => {
    if (!settlementId) return;
    navigate(`/admin/settlements/${settlementId}`);
  };

  return (
    <PageLayout
      title={pageTitle}
      description="정산 상태와 판매자 기준으로 정산을 조회하고, settlementId 앵커로 A4 상세 화면으로 이동합니다."
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
              style={{ minWidth: "240px", flex: "0 0 240px" }}
            >
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

              <Link
                to="/admin/settlement-batches"
                className="btn btn--secondary"
              >
                배치 콘솔(A2)
              </Link>
            </div>
          </div>
        </form>
      </SectionCard>

      {errorMessage ? (
        <>
          <GuardNotice title="조회 실패" message={errorMessage} tone="danger" />
          <ErrorState
            title="정산 리스트 조회 실패"
            description={errorMessage}
          />
        </>
      ) : null}

      <SectionCard title="정산 리스트" description={`총 ${rows.length}건`}>
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
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>settlementId</th>
                  <th>상태</th>
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
        )}
      </SectionCard>
    </PageLayout>
  );
}