// /admin-web/src/pages/_sample/SampleListPage.jsx

import PageLayout from "../../components/layout/PageLayout.jsx";
import SectionCard from "../../components/layout/SectionCard.jsx";
import StatusBadge from "../../components/display/StatusBadge.jsx";

export default function SampleListPage() {
  return (
    <PageLayout
      title="리스트형 페이지 샘플"
      description="검색 조건 + 테이블 + 페이지네이션 구조 예시"
    >
      <SectionCard title="검색 조건">
        <div className="filter-bar">
          <div className="form-field">
            <label className="form-field__label">merchantId</label>
            <input className="input" placeholder="MRC_1001" />
          </div>

          <div className="form-field">
            <label className="form-field__label">status</label>
            <select className="select">
              <option>전체</option>
              <option>READY</option>
              <option>HOLD_ACTIVE</option>
              <option>PAY_REQUESTED</option>
              <option>PAID</option>
            </select>
          </div>

          <div className="form-field search-field">
            <label className="form-field__label">keyword</label>
            <input className="input" placeholder="settlementId / paymentId" />
          </div>
        </div>

        <div className="button-row action-panel" style={{ marginTop: "16px" }}>
          <button type="button" className="btn btn--primary">
            조회
          </button>
          <button type="button" className="btn btn--secondary">
            초기화
          </button>
        </div>
      </SectionCard>

      <SectionCard title="목록">
        <div className="table-toolbar">
          <div>정산 관리 목록</div>
          <div className="action-panel">
            <button type="button" className="btn btn--secondary">
              CSV 다운로드
            </button>
            <button type="button" className="btn btn--primary">
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
                <th>net</th>
                <th>baseDate</th>
                <th>createdAt</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>
                  <div className="copyable-id">
                    <span className="copyable-id__text copyable-id__text--short">
                      SET-20260318-0001
                    </span>
                  </div>
                </td>
                <td>MRC_1001</td>
                <td><StatusBadge status="READY" /></td>
                <td><span className="amount-text">125,000원</span></td>
                <td>2026-03-18</td>
                <td>2026-03-18 10:30:00</td>
              </tr>

              <tr>
                <td>
                  <div className="copyable-id">
                    <span className="copyable-id__text copyable-id__text--short">
                      SET-20260318-0002
                    </span>
                  </div>
                </td>
                <td>MRC_1002</td>
                <td><StatusBadge status="HOLD_ACTIVE" /></td>
                <td><span className="amount-text">98,000원</span></td>
                <td>2026-03-18</td>
                <td>2026-03-18 11:05:00</td>
              </tr>

              <tr>
                <td>
                  <div className="copyable-id">
                    <span className="copyable-id__text copyable-id__text--short">
                      SET-20260318-0003
                    </span>
                  </div>
                </td>
                <td>MRC_1003</td>
                <td><StatusBadge status="PAID" /></td>
                <td><span className="amount-text">301,000원</span></td>
                <td>2026-03-18</td>
                <td>2026-03-18 12:20:00</td>
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
    </PageLayout>
  );
}