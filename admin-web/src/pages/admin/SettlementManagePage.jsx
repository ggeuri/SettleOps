import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import PageLayout from "../../components/layout/PageLayout.jsx";
import SectionCard from "../../components/layout/SectionCard.jsx";
import StatusBadge from "../../components/display/StatusBadge.jsx";

const MOCK_SETTLEMENTS = [
  {
    settlementId: "SET-20260318-0001",
    merchantId: "MRC_1001",
    status: "READY",
    gross: 125000,
    fee: 3750,
    vat: 375,
    net: 120875,
    baseDate: "2026-03-18",
    createdAt: "2026-03-18 10:30:00",
  },
  {
    settlementId: "SET-20260318-0002",
    merchantId: "MRC_1002",
    status: "HOLD_ACTIVE",
    gross: 98000,
    fee: 2940,
    vat: 294,
    net: 94766,
    baseDate: "2026-03-18",
    createdAt: "2026-03-18 11:05:00",
  },
  {
    settlementId: "SET-20260318-0003",
    merchantId: "MRC_1003",
    status: "PAY_REQUESTED",
    gross: 301000,
    fee: 9030,
    vat: 903,
    net: 291067,
    baseDate: "2026-03-18",
    createdAt: "2026-03-18 12:20:00",
  },
  {
    settlementId: "SET-20260317-0004",
    merchantId: "MRC_1001",
    status: "PAID",
    gross: 77000,
    fee: 2310,
    vat: 231,
    net: 74459,
    baseDate: "2026-03-17",
    createdAt: "2026-03-17 16:10:00",
  },
  {
    settlementId: "SET-20260317-0005",
    merchantId: "MRC_1002",
    status: "READY",
    gross: 54000,
    fee: 1620,
    vat: 162,
    net: 52218,
    baseDate: "2026-03-17",
    createdAt: "2026-03-17 18:40:00",
  },
];

function formatAmount(value) {
  return `${value.toLocaleString("ko-KR")}원`;
}

export default function SettlementManagePage() {
  const navigate = useNavigate();

  const [filters, setFilters] = useState({
    merchantId: "",
    status: "전체",
    keyword: "",
  });

  const filteredSettlements = useMemo(() => {
    return MOCK_SETTLEMENTS.filter((item) => {
      const matchMerchant =
        !filters.merchantId || item.merchantId.includes(filters.merchantId);

      const matchStatus =
        filters.status === "전체" ? true : item.status === filters.status;

      const keyword = filters.keyword.trim().toLowerCase();
      const matchKeyword =
        !keyword ||
        item.settlementId.toLowerCase().includes(keyword) ||
        item.merchantId.toLowerCase().includes(keyword);

      return matchMerchant && matchStatus && matchKeyword;
    });
  }, [filters]);

  const summary = useMemo(() => {
    return {
      totalCount: filteredSettlements.length,
      readyCount: filteredSettlements.filter((item) => item.status === "READY").length,
      holdCount: filteredSettlements.filter((item) => item.status === "HOLD_ACTIVE").length,
      payRequestedCount: filteredSettlements.filter((item) => item.status === "PAY_REQUESTED").length,
    };
  }, [filteredSettlements]);

  function handleChange(event) {
    const { name, value } = event.target;
    setFilters((prev) => ({
      ...prev,
      [name]: value,
    }));
  }

  function handleReset() {
    setFilters({
      merchantId: "",
      status: "전체",
      keyword: "",
    });
  }

  function handleRowClick(settlementId) {
    navigate(`/admin/settlements/${settlementId}`);
  }

  return (
    <PageLayout
      title="정산 관리"
      description="운영자가 merchantId / status 기준으로 정산을 조회하고 상세(A4)로 이동합니다."
    >
      <SectionCard title="요약">
        <div className="summary-card-grid">
          <div className="card">
            <div className="card__body">
              <div className="summary-card__label">총 정산 건수</div>
              <div className="summary-card__value">{summary.totalCount}</div>
            </div>
          </div>

          <div className="card">
            <div className="card__body">
              <div className="summary-card__label">READY</div>
              <div className="summary-card__value">{summary.readyCount}</div>
            </div>
          </div>

          <div className="card">
            <div className="card__body">
              <div className="summary-card__label">HOLD_ACTIVE</div>
              <div className="summary-card__value">{summary.holdCount}</div>
            </div>
          </div>

          <div className="card">
            <div className="card__body">
              <div className="summary-card__label">PAY_REQUESTED</div>
              <div className="summary-card__value">{summary.payRequestedCount}</div>
            </div>
          </div>
        </div>
      </SectionCard>

      <SectionCard title="검색 조건">
        <div className="filter-bar">
          <div className="form-field">
            <label className="form-field__label">merchantId</label>
            <input
              className="input"
              name="merchantId"
              value={filters.merchantId}
              onChange={handleChange}
              placeholder="MRC_1001"
            />
          </div>

          <div className="form-field">
            <label className="form-field__label">status</label>
            <select
              className="select"
              name="status"
              value={filters.status}
              onChange={handleChange}
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
              value={filters.keyword}
              onChange={handleChange}
              placeholder="settlementId / merchantId"
            />
          </div>
        </div>

        <div className="button-row action-panel" style={{ marginTop: "16px" }}>
          <button type="button" className="btn btn--primary">
            조회
          </button>
          <button type="button" className="btn btn--secondary" onClick={handleReset}>
            초기화
          </button>
        </div>
      </SectionCard>

      <SectionCard title="목록">
        <div className="table-toolbar">
          <div>정산 목록</div>
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
                <th>settlementId</th>
                <th>merchantId</th>
                <th>status</th>
                <th>gross</th>
                <th>fee</th>
                <th>vat</th>
                <th>net</th>
                <th>baseDate</th>
                <th>createdAt</th>
              </tr>
            </thead>
            <tbody>
              {filteredSettlements.length === 0 ? (
                <tr>
                  <td colSpan={9}>조회 결과가 없습니다.</td>
                </tr>
              ) : (
                filteredSettlements.map((item) => (
                  <tr
                    key={item.settlementId}
                    onClick={() => handleRowClick(item.settlementId)}
                    style={{ cursor: "pointer" }}
                    title={item.settlementId}
                  >
                    <td>
                      <div className="copyable-id">
                        <span className="copyable-id__text copyable-id__text--short">
                          {item.settlementId}
                        </span>
                      </div>
                    </td>
                    <td>{item.merchantId}</td>
                    <td>
                      <StatusBadge status={item.status} />
                    </td>
                    <td>
                      <span className="amount-text">{formatAmount(item.gross)}</span>
                    </td>
                    <td>
                      <span className="amount-text amount-text--negative">
                        -{formatAmount(item.fee)}
                      </span>
                    </td>
                    <td>
                      <span className="amount-text amount-text--negative">
                        -{formatAmount(item.vat)}
                      </span>
                    </td>
                    <td>
                      <span className="amount-text">{formatAmount(item.net)}</span>
                    </td>
                    <td>{item.baseDate}</td>
                    <td>{item.createdAt}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        <div className="pagination">
          <button type="button" className="btn btn--secondary">
            이전
          </button>
          <button type="button" className="btn btn--primary">
            1
          </button>
          <button type="button" className="btn btn--secondary">
            다음
          </button>
        </div>
      </SectionCard>
    </PageLayout>
  );
}