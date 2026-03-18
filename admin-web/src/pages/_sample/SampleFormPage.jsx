// /admin-web/src/pages/_sample/SampleFormPage.jsx

import PageLayout from "../../components/layout/PageLayout.jsx";
import SectionCard from "../../components/layout/SectionCard.jsx";

export default function SampleFormPage() {
  return (
    <PageLayout
      title="폼형 페이지 샘플"
      description="입력 필드 + 안내 문구 + 결과 카드 구조 예시"
    >
      <SectionCard title="입력 폼">
        <form className="form-stack">
          <div className="form-field">
            <label className="form-field__label">merchantId</label>
            <input className="input" placeholder="MRC_1001" />
          </div>

          <div className="form-field">
            <label className="form-field__label">buyerId</label>
            <input className="input" placeholder="BUYER_2001" />
          </div>

          <div className="form-field">
            <label className="form-field__label">itemName</label>
            <input className="input" placeholder="아이템명 입력" />
          </div>

          <div className="form-field">
            <label className="form-field__label">amount</label>
            <input
              className="input"
              type="number"
              min="1"
              step="1"
              placeholder="10000"
            />
          </div>

          <div className="form-field">
            <label className="form-field__label">comment</label>
            <textarea
              className="textarea"
              placeholder="운영 메모 또는 설명 입력"
            />
          </div>

          <div className="button-row">
            <button type="submit" className="btn btn--primary">
              저장
            </button>
            <button type="button" className="btn btn--secondary">
              취소
            </button>
          </div>
        </form>
      </SectionCard>

      <SectionCard title="에러 메시지 샘플">
        <div className="state-error">amount는 KRW 정수(원)만 입력 가능합니다.</div>
      </SectionCard>

      <SectionCard title="안내 문구 샘플">
        <div className="guard-notice">
          <div className="guard-notice__title">입력 규칙</div>
          <div className="guard-notice__description">
            금액은 KRW 정수만 허용하며, comment는 운영 액션에서 감사 추적용으로 사용될 수 있습니다.
          </div>
        </div>
      </SectionCard>

      <SectionCard title="결과 샘플">
        <div className="info-list">
          <div><strong>orderId</strong> ORD-20260318-0001</div>
          <div><strong>status</strong> CREATED</div>
          <div><strong>amount</strong> 10,000원</div>
        </div>
      </SectionCard>
    </PageLayout>
  );
}