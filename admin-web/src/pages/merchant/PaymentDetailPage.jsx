// /admin-web/src/pages/merchant/PaymentDetailPage.jsx

import PageLayout from "../../components/layout/PageLayout.jsx";
import SectionCard from "../../components/layout/SectionCard.jsx";
import StatusBadge from "../../components/display/StatusBadge.jsx";
import "../../style/payment-detail.css";

export default function PaymentDetailPage() {
  return (
    <PageLayout
      title="결제 상세"
      description="U3 결제 상세. event 타임라인과 환불 CTA 이동이 들어가는 상세 페이지입니다."
    >
      <div className="payment-detail-page">
        <SectionCard title="결제 요약">
          <div className="summary-card-grid payment-summary-grid">
            <div className="card summary-card payment-summary-card payment-summary-card--wide">
              <div className="card__body">
                <div className="summary-card__label">paymentId</div>
                <div className="summary-card__value payment-summary-card__value--id copyable-id__text">
                  PAY-20260318-0001-550e8400-e29b-41d4-a716-446655440000
                </div>

                <div className="payment-summary-card__meta">
                  <StatusBadge status="CAPTURED" />
                </div>
              </div>
            </div>

            <div className="card summary-card payment-summary-card">
              <div className="card__body">
                <div className="summary-card__label">amount / currency</div>
                <div className="summary-card__value">
                  125,000원 / KRW
                </div>
              </div>
            </div>
          </div>
        </SectionCard>

        <div className="page-grid-2">
          <SectionCard title="상세 정보">
            <div className="kv-list">
              <div className="kv-item">
                <div className="kv-item__label">orderId</div>
                <div className="kv-item__value">
                  <span className="display-field">ORD-20260318-0001</span>
                </div>
              </div>

              <div className="kv-item">
                <div className="kv-item__label">merchantId</div>
                <div className="kv-item__value">
                  <span className="display-field">MRC_1001</span>
                </div>
              </div>

              <div className="kv-item">
                <div className="kv-item__label">buyerId</div>
                <div className="kv-item__value">
                  <span className="display-field">BUYER_2001</span>
                </div>
              </div>

              <div className="kv-item">
                <div className="kv-item__label">amount</div>
                <div className="kv-item__value">
                  <span className="display-field amount-text">125,000원</span>
                </div>
              </div>

              <div className="kv-item">
                <div className="kv-item__label">capturedAt</div>
                <div className="kv-item__value">
                  <span className="display-field">2026-03-18 10:25:00</span>
                </div>
              </div>
            </div>
          </SectionCard>

          <SectionCard title="환불">
            <div className="page-section">
              <div className="kv-list">
                <div className="kv-item">
                  <div className="kv-item__label">환불 가능 금액</div>
                  <div className="kv-item__value">
                    <span className="display-field amount-text">125,000원</span>
                  </div>
                </div>

                <div className="kv-item">
                  <div className="kv-item__label">환불 처리중 금액</div>
                  <div className="kv-item__value">
                    <span className="display-field amount-text">0원</span>
                  </div>
                </div>
              </div>

              <div className="guard-notice">
                <div className="guard-notice__title">환불 이동</div>
                <div className="guard-notice__description">
                  이 paymentId 기준으로 refund-context 확인 후 환불 요청 화면(U6)으로 이동
                </div>
              </div>

              <div className="action-panel">
                <button type="button" className="btn btn--primary">환불 요청하기</button>
                <button type="button" className="btn btn--secondary">refund-context 확인</button>
              </div>
            </div>
          </SectionCard>
        </div>

        <SectionCard title="이벤트 타임라인">
          <div className="timeline">
            <div className="timeline-item">
              <div className="timeline-item__time">2026-03-18 10:20:00</div>
              <div className="timeline-item__body">
                <div className="timeline-item__title">PAYMENT_CREATED</div>
                <div className="timeline-item__meta">
                  actor: CONSUMER / entity: PAYMENT / id: PAY-20260318-0001
                </div>
              </div>
            </div>

            <div className="timeline-item">
              <div className="timeline-item__time">2026-03-18 10:25:00</div>
              <div className="timeline-item__body">
                <div className="timeline-item__title">PAYMENT_CAPTURED</div>
                <div className="timeline-item__meta">
                  actor: CONSUMER / entity: PAYMENT / id: PAY-20260318-0001
                </div>
              </div>
            </div>

            <div className="timeline-item">
              <div className="timeline-item__time">2026-03-18 13:05:00</div>
              <div className="timeline-item__body">
                <div className="timeline-item__title">PAYMENT_CONFIRMED</div>
                <div className="timeline-item__meta">
                  actor: CONSUMER / entity: PAYMENT / id: PAY-20260318-0001
                </div>
                <div className="audit-meta-block">
                  {`{
  "requestId": "req_20260318_confirm_001",
  "before": { "status": "CAPTURED" },
  "after": { "status": "CAPTURED" },
  "eventType": "PAYMENT_CONFIRMED"
}`}
                </div>
              </div>
            </div>
          </div>
        </SectionCard>

        <SectionCard title="정책 메모">
          <div className="info-list">
            <div><strong>LOCKED</strong> CONFIRMED는 payment.status가 아니라 payment_event로만 표현</div>
            <div><strong>LOCKED</strong> payment.status는 CREATED / CAPTURED만 사용</div>
            <div><strong>연결 화면</strong> U6 환불 요청·현황</div>
          </div>
        </SectionCard>
      </div>
    </PageLayout>
  );
}