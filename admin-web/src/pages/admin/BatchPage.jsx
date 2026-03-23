import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";

import PageLayout from "../../components/layout/PageLayout.jsx";
import SectionCard from "../../components/layout/SectionCard.jsx";
import ActionButton from "../../components/layout/ActionButton.jsx";
import StatusBadge from "../../components/display/StatusBadge.jsx";
import CopyableId from "../../components/display/CopyableId.jsx";
import InfoRow from "../../components/display/InfoRow.jsx";
import GuardNotice from "../../components/common/GuardNotice.jsx";
import EmptyState from "../../components/feedback/EmptyState.jsx";
import ErrorState from "../../components/feedback/ErrorState.jsx";
import LoadingBlock from "../../components/feedback/LoadingBlock.jsx";

import { formatDateTimeWithSeconds } from "../../utils/format.js";
import {
  getSettlementBatchHistory,
  runSettlementBatch,
} from "../../api/adminBatchApi.js";

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
  const status = error?.status;
  const message =
    error?.body?.message ||
    error?.body?.reason ||
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

function extractHistoryRunId(row) {
  if (row?.type === "SKIP") {
    return row?.skip?.runId || "-";
  }

  if (row?.type === "OK_FAIL") {
    return row?.batch?.runId || row?.batch?.batchId || "-";
  }

  return "-";
}

function buildHistoryRowKey(row, index) {
  if (row?.type === "SKIP") {
    return `SKIP-${
      row?.skip?.requestId || row?.skip?.occurredAt || row?.skip?.baseDate || index
    }`;
  }

  if (row?.type === "OK_FAIL") {
    return `OK_FAIL-${
      row?.batch?.runId ||
      row?.batch?.batchId ||
      row?.batch?.requestId ||
      row?.batch?.batchKey ||
      index
    }`;
  }

  return `ROW-${index}`;
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

function CopyableValue({ value }) {
  if (!value || value === "-") {
    return <span>-</span>;
  }

  return <CopyableId value={value} short />;
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
        const data = await getSettlementBatchHistory({
          from: queryFrom,
          to: queryTo,
          page: 0,
          size: 20,
        });

        const normalized = normalizeHistoryPayload(data);
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

      const data = await runSettlementBatch(baseDate);

      setRunResult(data || null);
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
            className="button-row action-panel"
            style={{
              display: "flex",
              alignItems: "center",
              gap: "12px",
              flexWrap: "wrap",
            }}
          >
            <ActionButton
              type="button"
              variant="primary"
              onClick={handleRunBatch}
              disabled={runLoading || !baseDate}
            >
              {runLoading ? "배치 실행 중..." : "배치 실행"}
            </ActionButton>

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
              <InfoRow label="result">
                <StatusBadge status={extractRunResult(runResult)} />
              </InfoRow>
              <InfoRow label="baseDate">
                {extractRunBaseDate(runResult, baseDate)}
              </InfoRow>
              <InfoRow label="runId">{extractRunId(runResult)}</InfoRow>
              <InfoRow label="requestId">
                {extractRunRequestId(runResult) ? (
                  <CopyableValue value={extractRunRequestId(runResult)} />
                ) : (
                  "-"
                )}
              </InfoRow>
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
              <div className="summary-card__label">현재 페이지 OK</div>
              <div className="summary-card__value">{summary.okCount}</div>
            </div>
          </div>

          <div className="card">
            <div className="card__body">
              <div className="summary-card__label">현재 페이지 FAIL</div>
              <div className="summary-card__value">{summary.failCount}</div>
            </div>
          </div>

          <div className="card">
            <div className="card__body">
              <div className="summary-card__label">현재 페이지 SKIP</div>
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
                  const rowKey = buildHistoryRowKey(row, index);
                  const runId = extractHistoryRunId(row);
                  const baseDateValue = extractHistoryBaseDate(row);
                  const result = extractHistoryResult(row);
                  const requestId = extractHistoryRequestId(row);
                  const triggeredBy = extractHistoryTriggeredBy(row);
                  const occurredAt = extractHistoryOccurredAt(row);

                  return (
                    <tr key={rowKey}>
                      <td style={{ verticalAlign: "middle" }}>
                        <CopyableValue value={runId} />
                      </td>
                      <td style={{ verticalAlign: "middle" }}>{baseDateValue}</td>
                      <td style={{ verticalAlign: "middle" }}>
                        <StatusBadge status={result} />
                      </td>
                      <td style={{ verticalAlign: "middle" }}>
                        {requestId ? <CopyableValue value={requestId} /> : "-"}
                      </td>
                      <td style={{ verticalAlign: "middle" }}>{triggeredBy}</td>
                      <td style={{ verticalAlign: "middle" }}>
                        {formatDateTimeWithSeconds(occurredAt)}
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