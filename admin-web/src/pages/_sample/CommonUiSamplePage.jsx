import PageLayout from "../../components/layout/PageLayout.jsx";
import SectionCard from "../../components/layout/SectionCard.jsx";
import ActionButton from "../../components/layout/ActionButton.jsx";
import StatusBadge from "../../components/display/StatusBadge.jsx";

export default function CommonUiSamplePage() {
  return (
    <PageLayout
      title="공통 UI 샘플"
      description="타이틀, 버튼, 배지, 폼, 테이블, 메타 JSON, 타임라인, 상태 블록 예시"
    >
      {/* =========================
          1. 상단 액션 버튼 샘플
         ========================= */}
      <SectionCard title="버튼 샘플">
        <div className="action-panel">
          <ActionButton>기본 버튼</ActionButton>
          <button type="button" className="btn btn--primary">
            Primary
          </button>
          <button type="button" className="btn btn--secondary">
            Secondary
          </button>
          <button type="button" className="btn btn--danger">
            Danger
          </button>
          <button type="button" className="btn btn--ghost">
            Ghost
          </button>
          <button type="button" className="btn btn--primary" disabled>
            Disabled
          </button>
        </div>
      </SectionCard>

      {/* =========================
          2. 상태 배지 / 토큰 샘플
         ========================= */}
      <SectionCard title="상태 / 배지 샘플">
        <div className="page-section">
          <div className="btn-group">
            <span className="badge badge--default">DEFAULT</span>
            <span className="badge badge--primary">READY</span>
            <span className="badge badge--success">PAID</span>
            <span className="badge badge--warning">HOLD_ACTIVE</span>
            <span className="badge badge--danger">FAILED</span>
          </div>

          <div className="btn-group">
            <StatusBadge status="CREATED" />
            <StatusBadge status="CAPTURED" />
            <StatusBadge status="PAY_REQUESTED" />
            <StatusBadge status="PAID" />
            <StatusBadge status="REQUESTED" />
            <StatusBadge status="APPROVED" />
            <StatusBadge status="REJECTED" />
          </div>
        </div>
      </SectionCard>

      {/* =========================
          3. ID / 금액 / 텍스트 샘플
         ========================= */}
      <SectionCard title="ID / 금액 표시 샘플">
        <div className="page-section">
          <div className="copyable-id">
            <span className="copyable-id__text copyable-id__text--short">
              SET-20260318-8b4c1a29-cf7d-4f98-bdf1-5d4f9aa21872
            </span>
            <button type="button" className="copyable-id__button">
              복사
            </button>
          </div>

          <div className="copyable-id">
            <span className="copyable-id__text">
              req_20260318_1a2b3c4d5e6f
            </span>
            <button type="button" className="copyable-id__button">
              복사
            </button>
          </div>

          <div className="page-section">
            <div>
              정산액: <span className="amount-text">125,000원</span>
            </div>
            <div>
              가산 항목:{" "}
              <span className="amount-text amount-text--positive">+132,000원</span>
            </div>
            <div>
              차감 항목:{" "}
              <span className="amount-text amount-text--negative">-7,000원</span>
            </div>
          </div>
        </div>
      </SectionCard>

      {/* =========================
          4. Guard / 안내 문구 샘플
         ========================= */}
      <SectionCard title="가드레일 안내 샘플">
        <div className="page-section">
          <div className="guard-notice">
            <div className="guard-notice__title">지급 요청 불가</div>
            <div className="guard-notice__description">
              Hold가 ACTIVE라 지급요청 불가입니다. Hold 큐(A5)에서 Release 후 다시 시도해야 합니다.
            </div>
          </div>

          <div className="guard-notice">
            <div className="guard-notice__title">차감정산 반영 대기</div>
            <div className="guard-notice__description">
              승인된 환불이 있어 차감정산 반영 전입니다. 다음 배치 실행 후 다시 시도해야 합니다.
            </div>
          </div>
        </div>
      </SectionCard>

      {/* =========================
          5. 폼 샘플
         ========================= */}
      <SectionCard title="폼 샘플">
        <div className="page-section">
          <div className="filter-bar">
            <div className="form-field">
              <label className="form-field__label">merchantId</label>
              <input className="input" placeholder="MRC_1234" />
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

            <div className="form-field">
              <label className="form-field__label">comment</label>
              <textarea
                className="textarea"
                placeholder="운영 메모 입력"
              />
            </div>
          </div>

          <div className="button-row action-panel">
            <button type="button" className="btn btn--primary">
              조회
            </button>
            <button type="button" className="btn btn--secondary">
              초기화
            </button>
          </div>
        </div>
      </SectionCard>

      {/* =========================
          6. 요약 카드 샘플
         ========================= */}
      <SectionCard title="요약 카드 샘플">
        <div className="summary-card-grid">
          <div className="card">
            <div className="card__body">
              <div className="summary-card__label">총 정산 건수</div>
              <div className="summary-card__value">128</div>
            </div>
          </div>

          <div className="card">
            <div className="card__body">
              <div className="summary-card__label">READY</div>
              <div className="summary-card__value">54</div>
            </div>
          </div>

          <div className="card">
            <div className="card__body">
              <div className="summary-card__label">HOLD_ACTIVE</div>
              <div className="summary-card__value">3</div>
            </div>
          </div>

          <div className="card">
            <div className="card__body">
              <div className="summary-card__label">PAID</div>
              <div className="summary-card__value">71</div>
            </div>
          </div>
        </div>
      </SectionCard>

      {/* =========================
          7. 테이블 샘플
         ========================= */}
      <SectionCard title="테이블 샘플">
        <div className="table-toolbar">
          <div>결제 / 정산 목록</div>
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
                <th>ID</th>
                <th>merchantId</th>
                <th>status</th>
                <th>amount</th>
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
                <td>
                  <span className="badge badge--primary">READY</span>
                </td>
                <td>
                  <span className="amount-text">125,000원</span>
                </td>
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
                <td>
                  <span className="badge badge--warning">HOLD_ACTIVE</span>
                </td>
                <td>
                  <span className="amount-text">98,000원</span>
                </td>
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
                <td>
                  <span className="badge badge--success">PAID</span>
                </td>
                <td>
                  <span className="amount-text">301,000원</span>
                </td>
                <td>2026-03-18</td>
                <td>2026-03-18 12:20:00</td>
              </tr>
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
            2
          </button>
          <button type="button" className="btn btn--secondary">
            다음
          </button>
        </div>
      </SectionCard>

      {/* =========================
          8. meta_json / audit 샘플
         ========================= */}
      <SectionCard title="meta_json 샘플">
        <div className="page-section">
          <div className="audit-meta-block">
{`{
  "requestId": "req_20260318_123456",
  "comment": "환불 승인 처리",
  "noOp": false,
  "before": {
    "status": "REQUESTED"
  },
  "after": {
    "status": "APPROVED"
  }
}`}
          </div>

          <div className="audit-meta-block">
{`{
  "noOp": true,
  "noOpReason": "ALREADY_APPROVED",
  "comment": "이미 승인된 환불 재요청"
}`}
          </div>
        </div>
      </SectionCard>

      {/* =========================
          9. 타임라인 샘플
         ========================= */}
      <SectionCard title="타임라인 샘플">
        <div className="timeline">
          <div className="timeline-item">
            <div className="timeline-item__time">2026-03-18 10:20:00</div>
            <div className="timeline-item__body">
              <div className="timeline-item__title">PAYMENT_CAPTURED</div>
              <div className="timeline-item__meta">
                actor: CONSUMER / entity: PAYMENT / id: pay_1234
              </div>
            </div>
          </div>

          <div className="timeline-item">
            <div className="timeline-item__time">2026-03-18 10:25:00</div>
            <div className="timeline-item__body">
              <div className="timeline-item__title">PAYMENT_CONFIRMED</div>
              <div className="timeline-item__meta">
                actor: CONSUMER / entity: PAYMENT / id: pay_1234
              </div>
            </div>
          </div>

          <div className="timeline-item">
            <div className="timeline-item__time">2026-03-19 02:00:00</div>
            <div className="timeline-item__body">
              <div className="timeline-item__title">SETTLEMENT_CREATED</div>
              <div className="timeline-item__meta">
                actor: SYSTEM / entity: SETTLEMENT / id: set_5678
              </div>
              <div className="audit-meta-block">
{`{
  "baseDate": "2026-03-18",
  "merchantId": "MRC_1001",
  "net": 125000
}`}
              </div>
            </div>
          </div>
        </div>
      </SectionCard>

      {/* =========================
          10. 상태 블록 샘플
         ========================= */}
      <SectionCard title="상태 블록 샘플">
        <div className="page-grid-2">
          <div className="state-block">
            <div className="state-block__title">조회 결과가 없습니다</div>
            <div className="state-block__description">
              검색 조건을 다시 확인하거나 기간을 넓혀서 조회해 주세요.
            </div>
          </div>

          <div className="state-block">
            <div className="state-block__title">로딩 중</div>
            <div className="state-block__description">
              데이터를 불러오고 있습니다.
            </div>
          </div>
        </div>
      </SectionCard>
    </PageLayout>
  );
}