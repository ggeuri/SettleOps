import SectionCard from "../layout/SectionCard.jsx";

export default function SettlementListSummarySection({
  totalCount,
  readyCount,
  holdCount,
  extraLabel,
  extraCount,
}) {
  return (
    <SectionCard title="요약">
      <div className="summary-card-grid">
        <div className="card">
          <div className="card__body">
            <div className="summary-card__label">총 정산 건수</div>
            <div className="summary-card__value">{totalCount}</div>
          </div>
        </div>

        <div className="card">
          <div className="card__body">
            <div className="summary-card__label">READY</div>
            <div className="summary-card__value">{readyCount}</div>
          </div>
        </div>

        <div className="card">
          <div className="card__body">
            <div className="summary-card__label">HOLD_ACTIVE</div>
            <div className="summary-card__value">{holdCount}</div>
          </div>
        </div>

        <div className="card">
          <div className="card__body">
            <div className="summary-card__label">{extraLabel}</div>
            <div className="summary-card__value">{extraCount}</div>
          </div>
        </div>
      </div>
    </SectionCard>
  );
}