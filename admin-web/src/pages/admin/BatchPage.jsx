import { useMemo, useState } from "react";
import PageLayout from "../../components/layout/PageLayout.jsx";
import SectionCard from "../../components/layout/SectionCard.jsx";
import StatusBadge from "../../components/display/StatusBadge.jsx";

const MOCK_BATCH_HISTORY = [
  {
    runId: "BATCH-RUN-20260318-0003",
    baseDate: "2026-03-18",
    result: "OK",
    triggeredBy: "admin01",
    startedAt: "2026-03-18 13:00:00",
    finishedAt: "2026-03-18 13:00:04",
    settlementCount: 3,
    message: "정산 배치가 정상 완료되었습니다.",
  },
  {
    runId: "BATCH-RUN-20260318-0002",
    baseDate: "2026-03-18",
    result: "SKIP",
    triggeredBy: "admin01",
    startedAt: "2026-03-18 12:40:00",
    finishedAt: "2026-03-18 12:40:00",
    settlementCount: 0,
    message: "동일 baseDate의 batch_key가 이미 존재하여 SKIP 처리되었습니다.",
  },
  {
    runId: "BATCH-RUN-20260317-0001",
    baseDate: "2026-03-17",
    result: "FAIL",
    triggeredBy: "admin02",
    startedAt: "2026-03-17 18:20:00",
    finishedAt: "2026-03-17 18:20:06",
    settlementCount: 2,
    message: "정합성 검증 실패로 FAIL 처리되었습니다.",
  },
];

export default function BatchPage() {
  const [form, setForm] = useState({
    baseDate: "2026-03-18",
  });

  const [lastRun, setLastRun] = useState(MOCK_BATCH_HISTORY[0]);

  const sortedHistory = useMemo(() => {
    return [...MOCK_BATCH_HISTORY];
  }, []);

  function handleChange(event) {
    const { name, value } = event.target;
    setForm((prev) => ({
      ...prev,
      [name]: value,
    }));
  }

  function handleRun() {
    setLastRun({
      runId: "BATCH-RUN-20260318-NEW",
      baseDate: form.baseDate,
      result: "OK",
      triggeredBy: "admin01",
      startedAt: "2026-03-18 14:10:00",
      finishedAt: "2026-03-18 14:10:05",
      settlementCount: 4,
      message: "목업 실행 결과입니다. 실제 API 연동 시 배치 실행 응답으로 교체됩니다.",
    });
  }

  return (
    <PageLayout
      title="배치 실행·이력"
      description="운영자가 baseDate 기준으로 정산 배치를 실행하고, 최근 실행 결과와 이력을 확인합니다."
    >
      <SectionCard title="실행 조건">
        <div className="filter-bar">
          <div className="form-field">
            <label className="form-field__label" htmlFor="baseDate">
              baseDate
            </label>
            <input
              id="baseDate"
              className="input"
              type="date"
              name="baseDate"
              value={form.baseDate}
              onChange={handleChange}
            />
          </div>
        </div>

        <div className="button-row action-panel" style={{ marginTop: "16px" }}>
          <button type="button" className="btn btn--primary" onClick={handleRun}>
            배치 실행
          </button>
        </div>
      </SectionCard>

      <SectionCard title="최근 실행 결과">
        <div className="summary-card-grid">
          <div className="card">
            <div className="card__body">
              <div className="summary-card__label">runId</div>
              <div className="summary-card__value" style={{ fontSize: "16px" }}>
                <div className="copyable-id">
                  <span className="copyable-id__text copyable-id__text--short">
                    {lastRun.runId}
                  </span>
                </div>
              </div>
            </div>
          </div>

          <div className="card">
            <div className="card__body">
              <div className="summary-card__label">baseDate</div>
              <div className="summary-card__value" style={{ fontSize: "16px" }}>
                {lastRun.baseDate}
              </div>
            </div>
          </div>

          <div className="card">
            <div className="card__body">
              <div className="summary-card__label">result</div>
              <div className="summary-card__value" style={{ fontSize: "16px" }}>
                <StatusBadge status={lastRun.result} />
              </div>
            </div>
          </div>

          <div className="card">
            <div className="card__body">
              <div className="summary-card__label">settlementCount</div>
              <div className="summary-card__value">{lastRun.settlementCount}</div>
            </div>
          </div>
        </div>

        <div className="table-wrap" style={{ marginTop: "16px" }}>
          <table className="data-table">
            <tbody>
              <tr>
                <th>triggeredBy</th>
                <td>{lastRun.triggeredBy}</td>
              </tr>
              <tr>
                <th>startedAt</th>
                <td>{lastRun.startedAt}</td>
              </tr>
              <tr>
                <th>finishedAt</th>
                <td>{lastRun.finishedAt}</td>
              </tr>
              <tr>
                <th>message</th>
                <td>{lastRun.message}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </SectionCard>

      <SectionCard title="실행 이력">
        <div className="table-toolbar">
          <div>최근 배치 실행 내역</div>
          <div className="action-panel">
            <button type="button" className="btn btn--secondary">
              새로고침
            </button>
          </div>
        </div>

        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>runId</th>
                <th>baseDate</th>
                <th>result</th>
                <th>triggeredBy</th>
                <th>startedAt</th>
                <th>finishedAt</th>
                <th>settlementCount</th>
                <th>message</th>
              </tr>
            </thead>
            <tbody>
              {sortedHistory.map((item) => (
                <tr key={item.runId}>
                  <td>
                    <div className="copyable-id">
                      <span className="copyable-id__text copyable-id__text--short">
                        {item.runId}
                      </span>
                    </div>
                  </td>
                  <td>{item.baseDate}</td>
                  <td>
                    <StatusBadge status={item.result} />
                  </td>
                  <td>{item.triggeredBy}</td>
                  <td>{item.startedAt}</td>
                  <td>{item.finishedAt}</td>
                  <td>{item.settlementCount}</td>
                  <td>{item.message}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="guard-notice" style={{ marginTop: "16px" }}>
          <strong>운영 메모</strong>
          <p style={{ marginTop: "8px" }}>
            SKIP 이력은 settlement_batch가 아니라 audit_log(BATCH_RUN_SKIPPED) 기반으로 노출됩니다.
          </p>
        </div>
      </SectionCard>
    </PageLayout>
  );
}