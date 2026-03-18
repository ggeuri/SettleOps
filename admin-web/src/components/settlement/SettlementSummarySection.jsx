import SectionCard from "../layout/SectionCard.jsx";
import StatusBadge from "../display/StatusBadge.jsx";

export default function SettlementSummarySection({
  settlementId,
  merchantId,
  status,
  baseDate,
}) {
  return (
    <SectionCard title="기본 정보">
      <div className="summary-card-grid">
        <div className="card">
          <div className="card__body">
            <div className="summary-card__label">settlementId</div>
            <div className="summary-card__value" style={{ fontSize: "16px" }}>
              <div className="copyable-id">
                <span className="copyable-id__text copyable-id__text--short">
                  {settlementId}
                </span>
              </div>
            </div>
          </div>
        </div>

        <div className="card">
          <div className="card__body">
            <div className="summary-card__label">merchantId</div>
            <div className="summary-card__value" style={{ fontSize: "16px" }}>
              {merchantId}
            </div>
          </div>
        </div>

        <div className="card">
          <div className="card__body">
            <div className="summary-card__label">status</div>
            <div className="summary-card__value" style={{ fontSize: "16px" }}>
              <StatusBadge status={status} />
            </div>
          </div>
        </div>

        <div className="card">
          <div className="card__body">
            <div className="summary-card__label">baseDate</div>
            <div className="summary-card__value" style={{ fontSize: "16px" }}>
              {baseDate}
            </div>
          </div>
        </div>
      </div>
    </SectionCard>
  );
}