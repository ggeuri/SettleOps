import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import axios from "axios";

import PageLayout from "../../components/layout/PageLayout.jsx";
import SectionCard from "../../components/layout/SectionCard.jsx";
import StatusBadge from "../../components/display/StatusBadge.jsx";
import CopyableId from "../../components/display/CopyableId.jsx";
import GuardNotice from "../../components/common/GuardNotice.jsx";
import EmptyState from "../../components/feedback/EmptyState.jsx";
import ErrorState from "../../components/feedback/ErrorState.jsx";
import LoadingBlock from "../../components/feedback/LoadingBlock.jsx";

function formatDate(date) {
  return date.toISOString().slice(0, 10);
}

function getDefaultFrom() {
  const date = new Date();
  date.setDate(date.getDate() - 6);
  return formatDate(date);
}

function getDefaultTo() {
  return formatDate(new Date());
}

function formatDateTime(value) {
  if (!value) return "-";

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);

  const yyyy = date.getFullYear();
  const mm = String(date.getMonth() + 1).padStart(2, "0");
  const dd = String(date.getDate()).padStart(2, "0");
  const hh = String(date.getHours()).padStart(2, "0");
  const mi = String(date.getMinutes()).padStart(2, "0");
  const ss = String(date.getSeconds()).padStart(2, "0");

  return `${yyyy}-${mm}-${dd} ${hh}:${mi}:${ss}`;
}

function normalizeHistoryPayload(data) {
  const page = data?.page ?? null;
  const content = Array.isArray(page?.content) ? page.content : [];

  return {
    rows: content,
    totalElements: Number(page?.totalElements ?? 0),
    numberOfElements: Number(page?.numberOfElements ?? content.length),
  };
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
    return "Admin 권한이 없어 배치 콘솔을 조회할 수 없습니다.";
  }

  if (status === 404) {
    return "배치 API 경로를 찾을 수 없습니다. 백엔드 라우팅을 확인해 주세요.";
  }

  return message || "배치 데이터를 불러오지 못했습니다.";
}

function extractRunRequestId(runResponse) {
  return runResponse?.requestId || null;
}

function extractRunResult(runResponse) {
  return (
    runResponse?.result ||
    runResponse?.batchResult ||
    runResponse?.status ||
    "OK"
  );
}

function extractRunBaseDate(runResponse, fallbackBaseDate) {
  return runResponse?.baseDate || fallbackBaseDate || "-";
}

function extractRunId(runResponse) {
  return (
    runResponse?.runId ||
    runResponse?.batchRunId ||
    runResponse?.batchId ||
    "-"
  );
}

function extractHistoryResult(row) {
  if (row?.type === "SKIP") return "SKIP";
  if (row?.type === "OK_FAIL") {
    return row?.batch?.result || "UNKNOWN";
  }
  return "UNKNOWN";
}

function extractHistoryRunId(row, index) {
  if (row?.type === "SKIP") {
    return row?.skip?.runId || `skip-row-${index}`;
  }

  if (row?.type === "OK_FAIL") {
    return row?.batch?.runId || row?.batch?.batchId || `okfail-row-${index}`;
  }

  return `row-${index}`;
}

function extractHistoryBaseDate(row) {
  if (row?.type === "SKIP") {
    return row?.skip?.baseDate || "-";
  }

  if (row?.type === "OK_FAIL") {
    return row?.batch?.batchKey || "-";
  }

  return "-";
}

function extractHistoryRequestId(row) {
  if (row?.type === "SKIP") {
    return row?.skip?.requestId || null;
  }

  if (row?.type === "OK_FAIL") {
    return row?.batch?.requestId || null;
  }

  return null;
}

function extractHistoryTriggeredBy(row) {
  if (row?.type === "SKIP") {
    return row?.skip?.actorId || "-";
  }

  if (row?.type === "OK_FAIL") {
    return row?.batch?.triggeredBy || "-";
  }

  return "-";
}

function extractHistoryOccurredAt(row) {
  if (row?.type === "SKIP") {
    return row?.skip?.occurredAt || row?.occurredAt || null;
  }

  if (row?.type === "OK_FAIL") {
    return (
      row?.batch?.finishedAt ||
      row?.batch?.createdAt ||
      row?.occurredAt ||
      null
    );
  }

  return row?.occurredAt || null;
}

export default function BatchPage() {
  const [baseDate, setBaseDate] = useState(getDefaultTo());
  const [fromDate, setFromDate] = useState(getDefaultFrom());
  const [toDate, setToDate] = useState(getDefaultTo());

  const [historyRows, setHistoryRows] = useState([]);
  const [historyTotal, setHistoryTotal] = useState(0);

  const [loading, setLoading] = useState(false);
  const [runLoading, setRunLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [runResult, setRunResult] = useState(null);

  const fetchHistory = useCallback(
    async (nextFromDate, nextToDate) => {
      const queryFrom = nextFromDate ?? fromDate;
      const queryTo = nextToDate ?? toDate;

      setLoading(true);
      setErrorMessage("");

      try {
        const response = await axios.get(
          "/api/admin/settlement-batches/history",
          {
            withCredentials: true,
            params: {
              from: queryFrom,
              to: queryTo,
              page: 0,
              size: 20,
            },
          }
        );

        const normalized = normalizeHistoryPayload(response.data);
        setHistoryRows(normalized.rows);
        setHistoryTotal(normalized.totalElements);
      } catch (error) {
        console.error("A2 배치 이력 조회 실패", error);
        setHistoryRows([]);
        setHistoryTotal(0);
        setErrorMessage(buildErrorMessage(error));
      } finally {
        setLoading(false);
      }
    },
    [fromDate, toDate]
  );

  useEffect(() => {
    fetchHistory();
  }, [fetchHistory]);

  const handleSearch = async (event) => {
    event.preventDefault();
    await fetchHistory();
  };

  const handleReset = async () => {
  const nextFrom = getDefaultFrom();
  const nextTo = getDefaultTo();

  setFromDate(nextFrom);
  setToDate(nextTo);

  await fetchHistory(nextFrom, nextTo);
};

  const handleRunBatch = async () => {
    if (!baseDate) return;

    try {
      setRunLoading(true);
      setErrorMessage("");
      setRunResult(null);

      const response = await axios.post(
        "/api/admin/settlement-batches/run",
        null,
        {
          withCredentials: true,
          params: { baseDate },
        }
      );

      setRunResult(response.data || null);
      await fetchHistory();
    } catch (error) {
      console.error("A2 배치 실행 실패", error);
      setErrorMessage(buildErrorMessage(error));
    } finally {
      setRunLoading(false);
    }
  };

  const summary = useMemo(() => {
    const total = historyTotal;
    const okCount = historyRows.filter(
      (row) => extractHistoryResult(row) === "OK"
    ).length;
    const failCount = historyRows.filter(
      (row) => extractHistoryResult(row) === "FAIL"
    ).length;
    const skipCount = historyRows.filter(
      (row) => extractHistoryResult(row) === "SKIP"
    ).length;

    return { total, okCount, failCount, skipCount };
  }, [historyRows, historyTotal]);

  return (
    <PageLayout
      title="배치 실행 · 이력"
      description="baseDate 기준으로 정산 배치를 실행하고 OK / FAIL / SKIP 이력을 운영 관점에서 확인합니다."
    >
      <SectionCard title="배치 실행">
        <div
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
            <label className="form-field__label" htmlFor="baseDate">
              baseDate
            </label>
            <input
              id="baseDate"
              className="input"
              type="date"
              value={baseDate}
              onChange={(event) => setBaseDate(event.target.value)}
            />
          </div>

          <div
            className="button-row"
            style={{
              display: "flex",
              alignItems: "center",
              gap: "12px",
              flexWrap: "wrap",
            }}
          >
            <button
              type="button"
              className="btn btn--primary"
              onClick={handleRunBatch}
              disabled={runLoading || !baseDate}
            >
              {runLoading ? "배치 실행 중..." : "배치 실행"}
            </button>

            <Link to="/admin/settlements" className="btn btn--secondary">
              정산 관리(A3)
            </Link>
          </div>
        </div>
      </SectionCard>

      {runResult ? (
        <GuardNotice
          title="배치 실행 결과"
          tone="success"
          message={
            <div className="info-list">
              <div>
                <strong>result</strong>{" "}
                <StatusBadge status={extractRunResult(runResult)} />
              </div>
              <div>
                <strong>baseDate</strong>{" "}
                {extractRunBaseDate(runResult, baseDate)}
              </div>
              <div>
                <strong>runId</strong> {extractRunId(runResult)}
              </div>
              <div>
                <strong>requestId</strong>{" "}
                {extractRunRequestId(runResult) ? (
                  <CopyableId value={extractRunRequestId(runResult)} short />
                ) : (
                  "-"
                )}
              </div>
            </div>
          }
        />
      ) : null}

      {errorMessage ? (
        <>
          <GuardNotice title="처리 실패" message={errorMessage} tone="danger" />
          <ErrorState title="배치 콘솔 처리 실패" description={errorMessage} />
        </>
      ) : null}

      <SectionCard title="이력 검색 조건">
        <form onSubmit={handleSearch}>
          <div
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
              className="button-row"
              style={{
                display: "flex",
                alignItems: "center",
                gap: "12px",
                flexWrap: "wrap",
              }}
            >
              <button
                type="submit"
                className="btn btn--primary"
                disabled={loading}
              >
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
            </div>
          </div>
        </form>
      </SectionCard>

      <SectionCard title="요약">
        <div className="summary-card-grid">
          <div className="card">
            <div className="card__body">
              <div className="summary-card__label">총 이력 건수</div>
              <div className="summary-card__value">{summary.total}</div>
            </div>
          </div>

          <div className="card">
            <div className="card__body">
              <div className="summary-card__label">OK</div>
              <div className="summary-card__value">{summary.okCount}</div>
            </div>
          </div>

          <div className="card">
            <div className="card__body">
              <div className="summary-card__label">FAIL</div>
              <div className="summary-card__value">{summary.failCount}</div>
            </div>
          </div>

          <div className="card">
            <div className="card__body">
              <div className="summary-card__label">SKIP</div>
              <div className="summary-card__value">{summary.skipCount}</div>
            </div>
          </div>
        </div>
      </SectionCard>

      <SectionCard title="배치 이력" description={`총 ${historyTotal}건`}>
        {loading ? (
          <LoadingBlock
            title="로딩 중"
            description="배치 이력을 불러오고 있습니다."
          />
        ) : historyRows.length === 0 ? (
          <EmptyState
            title="조회 결과가 없습니다"
            description="기간 조건을 다시 확인하거나 기본 기간으로 초기화해 주세요."
          />
        ) : (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>runId</th>
                  <th>baseDate</th>
                  <th>result</th>
                  <th>requestId</th>
                  <th>triggeredBy</th>
                  <th>occurredAt</th>
                </tr>
              </thead>
              <tbody>
                {historyRows.map((row, index) => {
                  const runId = extractHistoryRunId(row, index);
                  const baseDateValue = extractHistoryBaseDate(row);
                  const result = extractHistoryResult(row);
                  const requestId = extractHistoryRequestId(row);
                  const triggeredBy = extractHistoryTriggeredBy(row);
                  const occurredAt = extractHistoryOccurredAt(row);

                  return (
                    <tr key={`${row?.type || "ROW"}-${runId}-${index}`}>
                      <td>
                        <CopyableId value={runId} short />
                      </td>
                      <td>{baseDateValue}</td>
                      <td>
                        <StatusBadge status={result} />
                      </td>
                      <td>
                        {requestId ? (
                          <CopyableId value={requestId} short />
                        ) : (
                          "-"
                        )}
                      </td>
                      <td>{triggeredBy}</td>
                      <td>{formatDateTime(occurredAt)}</td>
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