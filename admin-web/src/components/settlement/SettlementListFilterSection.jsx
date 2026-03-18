import SectionCard from "../layout/SectionCard.jsx";

export default function SettlementListFilterSection({
  merchantId,
  status,
  keyword,
  onChange,
  onReset,
  merchantIdPlaceholder = "MRC_1001",
}) {
  return (
    <SectionCard title="검색 조건">
      <div className="filter-bar">
        <div className="form-field">
          <label className="form-field__label">merchantId</label>
          <input
            className="input"
            name="merchantId"
            value={merchantId}
            onChange={onChange}
            placeholder={merchantIdPlaceholder}
          />
        </div>

        <div className="form-field">
          <label className="form-field__label">status</label>
          <select
            className="select"
            name="status"
            value={status}
            onChange={onChange}
          >
            <option>전체</option>
            <option>READY</option>
            <option>HOLD_ACTIVE</option>
            <option>PAY_REQUESTED</option>
            <option>PAID</option>
          </select>
        </div>

        <div className="form-field search-field">
          <label className="form-field__label">keyword</label>
          <input
            className="input"
            name="keyword"
            value={keyword}
            onChange={onChange}
            placeholder="settlementId / merchantId"
          />
        </div>
      </div>

      <div className="button-row action-panel" style={{ marginTop: "16px" }}>
        <button type="button" className="btn btn--primary">
          조회
        </button>
        <button type="button" className="btn btn--secondary" onClick={onReset}>
          초기화
        </button>
      </div>
    </SectionCard>
  );
}