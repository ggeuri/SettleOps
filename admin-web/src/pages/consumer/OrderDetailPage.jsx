// /admin-web/src/pages/consumer/OrderDetailPage.jsx

import PageLayout from "../../components/layout/PageLayout.jsx";
import SectionCard from "../../components/layout/SectionCard.jsx";
import ActionButton from "../../components/layout/ActionButton.jsx";
import StatusBadge from "../../components/display/StatusBadge.jsx";

export default function OrderDetailPage() {
  return (
    <PageLayout
      title="결제 상세"
      description="C2 결제 상세(승인 버튼). orderId 기준 결제 승인과 멱등 UX가 들어갈 페이지입니다."
    >
      <SectionCard title="주문 / 결제 요약">
        <div className="kv-list">
          <div className="kv-item">
            <div className="kv-item__label">orderId</div>
            <div className="kv-item__value">
              <span className="display-field">ORD-20260318-0001</span>
            </div>
          </div>

          <div className="kv-item">
            <div className="kv-item__label">paymentId</div>
            <div className="kv-item__value">
              <span className="display-field copyable-id__text">PAY-20260318-0001</span>
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
            <div className="kv-item__label">itemName</div>
            <div className="kv-item__value">
              <span className="display-field">아이폰 14 프로</span>
            </div>
          </div>

          <div className="kv-item">
            <div className="kv-item__label">amount</div>
            <div className="kv-item__value">
              <span className="display-field amount-text">125,000원</span>
            </div>
          </div>

          <div className="kv-item">
            <div className="kv-item__label">paymentStatus</div>
            <div className="kv-item__value">
              <StatusBadge status="CREATED" />
            </div>
          </div>
        </div>
      </SectionCard>

      <SectionCard title="결제 승인">
        <div className="form-stack">
          <div className="form-field">
            <label className="form-field__label">X-Idempotency-Key</label>
            <input
              className="input"
              placeholder="멱등 키 입력"
              defaultValue="idem_20260318_demo_key"
            />
          </div>

          <div className="guard-notice">
            <div className="guard-notice__title">멱등 처리 안내</div>
            <div className="guard-notice__description">
              동일 orderId + X-Idempotency-Key 재요청은 no-op 200으로 현재 결과를 반환할 수 있습니다.
              성공 저장 전 경합 구간에서는 409 IN_PROGRESS가 반환될 수 있습니다.
            </div>
          </div>

          <div className="button-row action-panel">
            <ActionButton type="button">결제 승인</ActionButton>
            <button type="button" className="btn btn--secondary">멱등 키 복사</button>
          </div>
        </div>
      </SectionCard>

      <SectionCard title="응답 예시">
          <div className="kv-list">
            <div className="kv-item">
              <div className="kv-item__label">httpStatus</div>
              <div className="kv-item__value">
                <div className="display-field">200</div>
              </div>
            </div>

            <div className="kv-item">
              <div className="kv-item__label">requestId</div>
              <div className="kv-item__value">
                <div className="display-field">req_20260318_abc123</div>
              </div>
            </div>

            <div className="kv-item">
              <div className="kv-item__label">status</div>
              <div className="kv-item__value">
                <div className="display-field display-field--badge">
                  <span className="status-badge status-created">CREATED</span>
                </div>
              </div>
            </div>

            <div className="kv-item">
              <div className="kv-item__label">capturedAt</div>
              <div className="kv-item__value">
                <div className="display-field">2026-03-18 10:25:00</div>
              </div>
            </div>
          </div>
        </SectionCard>

      <SectionCard title="예외 / no-op 예시">
        <div className="page-section">
          <div className="guard-notice">
            <div className="guard-notice__title">400 BAD_REQUEST</div>
            <div className="guard-notice__description">
              X-Idempotency-Key 누락 시 400 처리
            </div>
          </div>

          <div className="guard-notice">
            <div className="guard-notice__title">409 IN_PROGRESS</div>
            <div className="guard-notice__description">
              성공 이력 저장 전 write 경합/중복 충돌 구간에서는 IN_PROGRESS 응답 가능
            </div>
          </div>

          <div className="guard-notice">
            <div className="guard-notice__title">409 ORDER_ALREADY_PAID</div>
            <div className="guard-notice__description">
              이미 PAID 상태인 order에 대해 pay 재요청 시 차단
            </div>
          </div>
        </div>
      </SectionCard>

      <SectionCard title="결제 이벤트 미니 타임라인">
        <div className="table-toolbar">
          <div>Pay 실행 이벤트 흐름</div>
          <div className="action-panel">
            <span className="badge badge--default">목업</span>
            <span className="badge badge--primary">replay 포함</span>
          </div>
        </div>

        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>실행일시</th>
                <th>eventType</th>
                <th>status</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>2026-03-18 10:24:58</td>
                <td>
                  <span className="badge badge--default">PAYMENT_CREATED</span>
                </td>
                <td>CREATED</td>
              </tr>

              <tr>
                <td>2026-03-18 10:25:00</td>
                <td>
                  <span className="badge badge--success">PAYMENT_CAPTURED</span>
                </td>
                <td>CAPTURED</td>
              </tr>

              <tr>
                <td>2026-03-18 10:25:03</td>
                <td>
                  <span className="badge badge--primary">REPLAY</span>
                </td>
                <td>CAPTURED</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div className="guard-notice" style={{ marginTop: "16px" }}>
          <div className="guard-notice__title">표시 규칙</div>
          <div className="guard-notice__description">
            최초 성공 시 PAYMENT_CREATED → PAYMENT_CAPTURED 흐름을 표시하고, 동일 멱등 키 재요청은
            replay/no-op 형태로 현재 결과를 재표시합니다.
          </div>
        </div>
      </SectionCard>

    </PageLayout>
  );
}