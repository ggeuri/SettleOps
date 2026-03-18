import { useMemo, useState } from "react";
import PageLayout from "../../components/layout/PageLayout.jsx";
import SectionCard from "../../components/layout/SectionCard.jsx";
import StatusBadge from "../../components/display/StatusBadge.jsx";

const MOCK_ORDERS = [
  {
    orderId: "ORD-20260318-0001",
    paymentId: "PAY-20260318-0001",
    itemName: "아이패드 프로 11",
    amount: 850000,
    orderStatus: "PAID",
    paymentStatus: "CAPTURED",
    confirmed: true,
    createdAt: "2026-03-18 10:20:00",
  },
  {
    orderId: "ORD-20260318-0002",
    paymentId: "PAY-20260318-0002",
    itemName: "에어팟 프로 2",
    amount: 310000,
    orderStatus: "PAID",
    paymentStatus: "CAPTURED",
    confirmed: false,
    createdAt: "2026-03-18 11:10:00",
  },
  {
    orderId: "ORD-20260317-0003",
    paymentId: null,
    itemName: "기계식 키보드",
    amount: 120000,
    orderStatus: "CREATED",
    paymentStatus: null,
    confirmed: false,
    createdAt: "2026-03-17 18:00:00",
  },
];

function formatAmount(value) {
  return `${value.toLocaleString("ko-KR")}원`;
}

export default function MyOrdersPage() {
  const [filters, setFilters] = useState({
    status: "전체",
    keyword: "",
  });

  const filteredOrders = useMemo(() => {
    return MOCK_ORDERS.filter((item) => {
      const matchStatus =
        filters.status === "전체" ? true : item.orderStatus === filters.status;

      const keyword = filters.keyword.trim().toLowerCase();
      const matchKeyword =
        !keyword ||
        item.orderId.toLowerCase().includes(keyword) ||
        item.itemName.toLowerCase().includes(keyword) ||
        (item.paymentId || "").toLowerCase().includes(keyword);

      return matchStatus && matchKeyword;
    });
  }, [filters]);

  function handleChange(event) {
    const { name, value } = event.target;
    setFilters((prev) => ({
      ...prev,
      [name]: value,
    }));
  }

  function handleReset() {
    setFilters({
      status: "전체",
      keyword: "",
    });
  }

  return (
    <PageLayout
      title="내 주문/결제 내역"
      description="주문, 결제, CONFIRMED 표시를 한 화면에서 확인합니다."
    >
      <SectionCard title="검색 조건">
        <div className="filter-bar">
          <div className="form-field">
            <label className="form-field__label">orderStatus</label>
            <select
              className="select"
              name="status"
              value={filters.status}
              onChange={handleChange}
            >
              <option>전체</option>
              <option>CREATED</option>
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
              placeholder="orderId / paymentId / itemName"
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
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>orderId</th>
                <th>paymentId</th>
                <th>itemName</th>
                <th>amount</th>
                <th>orderStatus</th>
                <th>paymentStatus</th>
                <th>confirmed</th>
                <th>createdAt</th>
              </tr>
            </thead>
            <tbody>
              {filteredOrders.length === 0 ? (
                <tr>
                  <td colSpan={8}>조회 결과가 없습니다.</td>
                </tr>
              ) : (
                filteredOrders.map((item) => (
                  <tr key={item.orderId}>
                    <td>
                      <div className="copyable-id">
                        <span className="copyable-id__text copyable-id__text--short">
                          {item.orderId}
                        </span>
                      </div>
                    </td>
                    <td>
                      {item.paymentId ? (
                        <div className="copyable-id">
                          <span className="copyable-id__text copyable-id__text--short">
                            {item.paymentId}
                          </span>
                        </div>
                      ) : (
                        "-"
                      )}
                    </td>
                    <td>{item.itemName}</td>
                    <td>
                      <span className="amount-text">{formatAmount(item.amount)}</span>
                    </td>
                    <td>
                      <StatusBadge status={item.orderStatus} />
                    </td>
                    <td>{item.paymentStatus || "-"}</td>
                    <td>
                      {item.confirmed ? (
                        <StatusBadge status="CONFIRMED" />
                      ) : (
                        <span>-</span>
                      )}
                    </td>
                    <td>{item.createdAt}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </SectionCard>
    </PageLayout>
  );
}