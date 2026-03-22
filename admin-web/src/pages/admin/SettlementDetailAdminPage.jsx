import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
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
  if (value === null || value === undefined || value === "") {
    return "-";
  }

  const numberValue = Number(value);
  if (Number.isNaN(numberValue)) {
    return String(value);
  }

  return `${numberValue.toLocaleString("ko-KR")}원`;
}

function normalizeSettlementList(data) {
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.content)) return data.content;
  if (Array.isArray(data?.items)) return data.items;
  if (Array.isArray(data?.data)) return data.data;
  return [];
}

function extractMerchantId(meData) {
  if (!meData) return null;

  return (
    meData.merchantId ||
    meData.loginId ||
    meData.userId ||
    meData.username ||
    null
  );
}

function buildErrorMessage(error) {
  const status = error?.response?.status;
  const message =
    error?.response?.data?.message ||
    error?.response?.data?.reason ||
    error?.message;

  if (status === 401) {
    return "인증이 만료되었거나 로그인되지 않았습니다. Merchant 세션을 다시 생성해 주세요.";
  }

  if (status === 403) {
    return "본인 판매자 정산 내역만 조회할 수 있습니다.";
  }

  if (status === 404) {
    return "정산 리스트 API 경로를 찾을 수 없습니다. 백엔드 라우팅을 확인해 주세요.";
  }

  if (status >= 500) {
    return "서버 오류로 정산 리스트를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.";
  }

  return message || "정산 리스트를 불러오지 못했습니다.";
}

export default function SettlementListPage() {
  const navigate = useNavigate();

  const [merchantId, setMerchantId] = useState("");
  const [status, setStatus] = useState("");

  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  const loadMerchantId = useCallback(async () => {
    const response = await axios.get("/api/me", {
      withCredentials: true,
    });

    const resolvedMerchantId = extractMerchantId(response.data);

    if (!resolvedMerchantId) {
      throw new Error("현재 로그인한 merchantId를 확인할 수 없습니다.");
    }

    setMerchantId(resolvedMerchantId);
    return resolvedMerchantId;
  }, []);

  const fetchSettlements = useCallback(async () => {
    setLoading(true);
    setErrorMessage("");

    try {
      const currentMerchantId = merchantId || (await loadMerchantId());

      const response = await axios.get(
        `/api/merchants/${currentMerchantId}/settlements`,
        {
          withCredentials: true,
        }
      );

      const list = normalizeSettlementList(response.data);
      setRows(list);
    } catch (error) {
      console.error("U4 정산 리스트 조회 실패", error);
      setRows([]);
      setErrorMessage(buildErrorMessage(error));
    } finally {
      setLoading(false);
    }
  }, [merchantId, loadMerchantId]);

  useEffect(() => {
    fetchSettlements();
  }, [fetchSettlements]);

  const filteredRows = useMemo(() => {
    if (!status) return rows;
    return rows.filter((row) => row?.status === status);
  }, [rows, status]);

  const handleSearch = async (event) => {
    event.preventDefault();
    await fetchSettlements();
  };

  const handleReset = async () => {
    setStatus("");
    await fetchSettlements();
  };

  const handleGoDetail = (settlementId) => {
    if (!settlementId) return;
    navigate(`/merchant/settlements/${settlementId}`);
  };

  return (
    <PageLayout
      title="정산 조회"
      description="판매자 정산 내역을 조회하고 settlementId 기준으로 상세 화면으로 이동합니다."
    >
      <SectionCard
        title="U4 정산 리스트"
        description="본인 판매자의 정산 건을 조회합니다."
      >
        <form className="filter-bar" onSubmit={handleSearch}>
          <div className="form-field">
            <label htmlFor="status">상태</label>
            <select
              id="status"
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
            <label htmlFor="merchantId">merchantId</label>
            <input
              id="merchantId"
              type="text"
              value={merchantId}
              disabled
              placeholder="로그인 세션 기준"
            />
          </div>

          <div className="button-row">
            <button
              type="submit"
              className="action-button primary"
              disabled={loading}
            >
              {loading ? "조회 중..." : "조회"}
            </button>

            <button
              type="button"
              className="action-button secondary"
              onClick={handleReset}
              disabled={loading}
            >
              초기화
            </button>
          </div>
        </form>
      </SectionCard>

      {errorMessage ? (
        <GuardNotice
          title="조회 실패"
          message={errorMessage}
          tone="danger"
        />
      ) : null}

      {!errorMessage && status ? (
        <GuardNotice
          title="상태 필터 안내"
          message="현재 status 필터는 프론트 표시 기준으로만 적용됩니다. 백엔드 status 필터 API가 연결되면 서버 필터로 전환할 수 있습니다."
          tone="warning"
        />
      ) : null}

      <SectionCard title="정산 목록" description={`총 ${filteredRows.length}건`}>
        {loading ? (
          <div className="empty-state">불러오는 중...</div>
        ) : filteredRows.length === 0 ? (
          <div className="empty-state">조회 결과가 없습니다.</div>
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
                {filteredRows.map((row, index) => {
                  const settlementId = row.settlementId ?? row.id ?? `row-${index}`;

                  return (
                    <tr
                      key={settlementId}
                      onClick={() => handleGoDetail(settlementId)}
                      style={{ cursor: settlementId ? "pointer" : "default" }}
                    >
                      <td>
                        {settlementId ? <CopyableId value={settlementId} /> : "-"}
                      </td>
                      <td>
                        <StatusBadge status={row.status || "UNKNOWN"} />
                      </td>
                      <td>{row.baseDate ?? "-"}</td>
                      <td>{row.merchantId ?? merchantId ?? "-"}</td>
                      <td>{formatAmount(row.gross)}</td>
                      <td>{formatAmount(row.fee)}</td>
                      <td>{formatAmount(row.vat)}</td>
                      <td>{formatAmount(row.net)}</td>
                      <td>
                        <button
                          type="button"
                          className="action-button secondary small"
                          onClick={(event) => {
                            event.stopPropagation();
                            handleGoDetail(settlementId);
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