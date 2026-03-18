// /admin-web/src/pages/_sample/SampleDetailPage.jsx

import PageLayout from "../../components/layout/PageLayout.jsx";
import SectionCard from "../../components/layout/SectionCard.jsx";
import StatusBadge from "../../components/display/StatusBadge.jsx";

export default function SampleDetailPage() {
  return (
    <PageLayout
      title="상세형 페이지 샘플"
      description="요약 카드 + 상세 정보 + 관련 섹션 + 운영 액션 구조 예시"
    >
      <SectionCard title="상단 액션">
        <div className="action-panel">
          <button type="button" className="btn btn--primary">지급 요청</button>
          <button type="button" className="btn btn--secondary">Trace로 보기</button>
          <button type="button" className="btn btn--secondary">Hold 큐로 이동</button>
          <button type="button" className="btn btn--secondary">Refund 큐로 이동</button>
        </div>
      </SectionCard>

      <SectionCard title="요약">
        <div className="summary-card-grid">
          <div className="card">
            <div className="card__body">
              <div className="summary-card__label">settlementId</div>
              <div className="summary-card__value">SET-20260318-0001</div>
            </div>
          </div>

          <div className="card">
            <div className="card__body">
              <div className="summary-card__label">status</div>
              <div className="summary-card__value">
                <StatusBadge status="READY" />
              </div>
            </div>
          </div>

          <div className="card">
            <div className="card__body">
              <div className="summary-card__label">merchantId</div>
              <div className="summary-card__value">MRC_1001</div>
            </div>
          </div>

          <div className="card">
            <div className="card__body">
              <div className="summary-card__label">net</div>
              <div className="summary-card__value">125,000원</div>
            </div>
          </div>
        </div>
      </SectionCard>

      <div className="page-grid-2">
        <SectionCard title="정산 상세">
          <div className="info-list">
            <div><strong>baseDate</strong> 2026-03-18</div>
            <div><strong>gross</strong> 132,000원</div>
            <div><strong>fee</strong> 5,000원</div>
            <div><strong>vat</strong> 2,000원</div>
            <div><strong>net</strong> 125,000원</div>
          </div>
        </SectionCard>

        <SectionCard title="운영 가드레일">
          <div className="guard-notice">
            <div className="guard-notice__title">지급 요청 가능</div>
            <div className="guard-notice__description">
              현재 HOLD_ACTIVE 및 REFUND_ADJUSTMENT_PENDING 조건이 없어 지급 요청이 가능합니다.
            </div>
          </div>
        </SectionCard>
      </div>

      <SectionCard title="정산 수식 뷰">
        <div className="info-list">
          <div>[+] gross: 132,000원</div>
          <div>[-] fee: 5,000원</div>
          <div>[-] vat: 2,000원</div>
          <div>[-] refund: 0원</div>
          <div><strong>[=] net: 125,000원</strong></div>
        </div>
      </SectionCard>

      <SectionCard title="연결 정보">
        <div className="info-list">
          <div>
            <strong>paymentId</strong>{" "}
            <span className="copyable-id__text">PAY-3bb2d7e4-7b18-4df8</span>
          </div>
          <div>
            <strong>requestId</strong>{" "}
            <span className="copyable-id__text">req_20260318_abc123</span>
          </div>
          <div>
            <strong>hold</strong> 없음
          </div>
          <div>
            <strong>refund</strong> 없음
          </div>
        </div>
      </SectionCard>

      <SectionCard title="라인 목록">
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>type</th>
                <th>paymentId</th>
                <th>amount</th>
                <th>createdAt</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>PAYMENT</td>
                <td>PAY-20260318-0001</td>
                <td>125,000원</td>
                <td>2026-03-19 02:00:00</td>
              </tr>
            </tbody>
          </table>
        </div>
      </SectionCard>
    </PageLayout>
  );
}