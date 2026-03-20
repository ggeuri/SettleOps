// /admin-web/src/pages/merchant/PaymentListPage.jsx

import PageLayout from "../../components/layout/PageLayout.jsx";
import SectionCard from "../../components/layout/SectionCard.jsx";
import StatusBadge from "../../components/display/StatusBadge.jsx";
import { Link } from "react-router-dom";

export default function PaymentListPage() {
  return (
    <PageLayout
      title="결제 조회"
      description="U2 결제 조회/검색. row 클릭 시 U3 결제 상세로 이동하는 리스트형 페이지입니다."
    >
      <SectionCard title="검색 조건">
        <div className="filter-bar">
          <div className="form-field">
            <label className="form-field__label">status</label>
            <select className="select">
              <option>전체</option>
              <option>CREATED</option>
              <option>CAPTURED</option>
            </select>
          </div>

          <div className="form-field">
            <label className="form-field__label">from</label>
            <input className="input" type="date" />
          </div>

          <div className="form-field">
            <label className="form-field__label">to</label>
            <input className="input" type="date" />
          </div>

          <div className="form-field search-field">
            <label className="form-field__label">keyword</label>
            <input className="input" placeholder="paymentId / orderId / itemName" />
          </div>
        </div>

        <div className="button-row action-panel" style={{ marginTop: "16px" }}>
          <button type="button" className="btn btn--primary">조회</button>
          <button type="button" className="btn btn--secondary">초기화</button>
        </div>
      </SectionCard>

      <SectionCard title="결제 목록">
        <div className="table-toolbar">
          <div>merchant 결제 목록</div>
          <div className="action-panel">
            <button type="button" className="btn btn--secondary">새로고침</button>
          </div>
        </div>

        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>paymentId</th>
                <th>orderId</th>
                <th>itemName</th>
                <th>status</th>
                <th>capturedAmount</th>
                <th>capturedAt</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>
                  <Link to="/merchant/payments/PAY-20260318-0001">
                    PAY-20260318-0001
                  </Link>
                </td>
                <td>ORD-20260318-0001</td>
                <td>아이폰 14 프로</td>
                <td><StatusBadge status="CAPTURED" /></td>
                <td>125,000원</td>
                <td>2026-03-18 10:25:00</td>
              </tr>
              <tr>
                <td>PAY-20260318-0002</td>
                <td>ORD-20260318-0002</td>
                <td>맥북 에어</td>
                <td><StatusBadge status="CAPTURED" /></td>
                <td>980,000원</td>
                <td>2026-03-18 11:10:00</td>
              </tr>
              <tr>
                <td>PAY-20260318-0003</td>
                <td>ORD-20260318-0003</td>
                <td>에어팟 프로</td>
                <td><StatusBadge status="CREATED" /></td>
                <td>0원</td>
                <td>-</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div className="pagination">
          <button type="button" className="btn btn--secondary">이전</button>
          <button type="button" className="btn btn--primary">1</button>
          <button type="button" className="btn btn--secondary">2</button>
          <button type="button" className="btn btn--secondary">다음</button>
        </div>
      </SectionCard>

      <SectionCard title="페이지 규칙">
        <div className="info-list">
          <div><strong>역할</strong> Merchant</div>
          <div><strong>핵심 이동</strong> U2 row 클릭 → U3 결제 상세(paymentId 전달)</div>
          <div><strong>표시 기준</strong> payment.status는 CREATED / CAPTURED만 사용</div>
        </div>
      </SectionCard>
    </PageLayout>
  );
}