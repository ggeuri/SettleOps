import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate, Link, useSearchParams } from "react-router-dom";
import axios from "axios";

import PageLayout from "../../components/layout/PageLayout.jsx";
import SectionCard from "../../components/layout/SectionCard.jsx";
import StatusBadge from "../../components/display/StatusBadge.jsx";
import GuardNotice from "../../components/common/GuardNotice.jsx";
import CopyableId from "../../components/common/CopyableId.jsx";

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
  return `${num.toLocaleString("ko-KR")}원`;
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

  return message || "정산 리스트를 불러오지 못했습니다. 백엔드 연결 상태와 API 응답 구조를 확인해 주세요.";
}

export default function SettlementManagePage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  const [status, setStatus] = useState(searchParams.get("status") ?? "");
  const [merchantId, setMerchantId] = useState(searchParams.get("merchantId") ?? "");

  const pageTitle = useMemo(() => "A3 정산 관리", []);

  const fetchSettlements = useCallback(
    async (nextStatus = status, nextMerchantId = merchantId) => {
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
    [status, merchantId]
  );

  useEffect(() => {
    const nextStatus = searchParams.get("status") ?? "";
    const nextMerchantId = searchParams.get("merchantId") ?? "";

    setStatus(nextStatus);
    setMerchantId(nextMerchantId);
    fetchSettlements(nextStatus, nextMerchantId);
  }, [searchParams, fetchSettlements]);

  const handleSearch = async (event) => {
    event.preventDefault();

    const nextParams = {};
    if (status) nextParams.status = status;
    if (merchantId.trim()) nextParams.merchantId = merchantId.trim();

    setSearchParams(nextParams);
  };

  const handleReset = async () => {
    setStatus("");
    setMerchantId("");
    setSearchParams({});
    await fetchSettlements("", "");
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
        <form className="filter-bar" onSubmit={handleSearch}>
          <div className="form-field">
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

          <div className="button-row action-panel">
            <button type="submit" className="btn btn--primary" disabled={loading}>
              {loading ? "조회 중..." : "조회"}
            </button>

            <button
              type="button"
              className="btn btn--secondary"
              onClick={handleReset}
              disabled={loading}
            >
              초기화
            </button>

            <Link to="/admin/settlement-batches" className="btn btn--secondary">
              배치 콘솔(A2)
            </Link>
          </div>
        </form>
      </SectionCard>

      {errorMessage ? (
        <GuardNotice title="조회 실패" message={errorMessage} tone="danger" />
      ) : null}

      <SectionCard title="정산 리스트" description={`총 ${rows.length}건`}>
        {loading ? (
          <div className="state-block">
            <div className="state-block__title">로딩 중</div>
            <div className="state-block__description">
              정산 리스트를 불러오고 있습니다.
            </div>
          </div>
        ) : rows.length === 0 ? (
          <div className="state-block">
            <div className="state-block__title">조회 결과가 없습니다</div>
            <div className="state-block__description">
              검색 조건을 다시 확인하거나 필터를 초기화해 주세요.
            </div>
          </div>
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
                  const settlementId = row?.settlementId ?? row?.id ?? `row-${index}`;

                  return (
                    <tr
                      key={settlementId}
                      onClick={() => handleRowClick(settlementId)}
                      style={{ cursor: settlementId ? "pointer" : "default" }}
                    >
                      <td onClick={(event) => event.stopPropagation()}>
                        <CopyableId value={settlementId} short />
                      </td>
                      <td>
                        <StatusBadge status={row?.status || "UNKNOWN"} />
                      </td>
                      <td>{row?.baseDate ?? "-"}</td>
                      <td>{row?.merchantId ?? "-"}</td>
                      <td>{formatAmount(row?.gross)}</td>
                      <td>{formatAmount(row?.fee)}</td>
                      <td>{formatAmount(row?.vat)}</td>
                      <td>{formatAmount(row?.net)}</td>
                      <td>
                        <button
                          type="button"
                          className="btn btn--secondary"
                          onClick={(event) => {
                            event.stopPropagation();
                            handleRowClick(settlementId);
                          }}
                          disabled={!settlementId}
                        >
                          상세조회
                        </button>
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