// /admin-web/src/pages/merchant/PaymentDetailPage.jsx

import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  getPaymentDetail,
  getRefundContext,
} from "../../api/merchantPaymentApi.js";
import { getMe } from "../../api/meApi.js";
import { formatDateTimeWithSeconds, formatKrw } from "../../util/format.js";
import PageLayout from "../../components/layout/PageLayout.jsx";
import SectionCard from "../../components/layout/SectionCard.jsx";
import StatusBadge from "../../components/display/StatusBadge.jsx";
import RequireLoginNotice from "../../components/common/RequireLoginNotice.jsx";
import "../../style/payment-detail.css";

function isMerchantRole(me) {
  if (!me) {
    return false;
  }

  if (me.role === "MERCHANT" || me.role === "ROLE_MERCHANT") {
    return true;
  }

  if (Array.isArray(me.authorities) && me.authorities.includes("ROLE_MERCHANT")) {
    return true;
  }

  return false;
}

function buildErrorInfo(error, fallbackMessage) {
  return {
    status: error?.status ?? null,
    message:
      error?.body?.message ||
      error?.body?.reason ||
      fallbackMessage,
  };
}

export default function PaymentDetailPage() {
  const { paymentId } = useParams();
  const navigate = useNavigate();

  const [paymentDetail, setPaymentDetail] = useState(null);
  const [refundContext, setRefundContext] = useState(null);
  const [loading, setLoading] = useState(true);
  const [refundLoading, setRefundLoading] = useState(false);
  const [errorInfo, setErrorInfo] = useState(null);
  const [refundErrorMessage, setRefundErrorMessage] = useState("");

  async function loadPaymentDetail() {
    const data = await getPaymentDetail(paymentId);
    setPaymentDetail(data);
  }

  useEffect(() => {
    async function loadPage() {
      try {
        setLoading(true);
        setErrorInfo(null);

        if (!paymentId) {
          setPaymentDetail(null);
          setErrorInfo({
            status: 404,
            message: "paymentId가 없습니다.",
          });
          return;
        }

        const meData = await getMe();

        if (!isMerchantRole(meData)) {
          setPaymentDetail(null);
          setErrorInfo({
            status: 403,
            message: "Merchant 권한이 필요한 페이지입니다.",
          });
          return;
        }

        await loadPaymentDetail();
      } catch (error) {
        setPaymentDetail(null);
        setErrorInfo(buildErrorInfo(error, "결제 상세 정보를 불러오지 못했습니다."));
      } finally {
        setLoading(false);
      }
    }

    loadPage();
  }, [paymentId]);

  async function handleLoadRefundContext() {
    try {
      setRefundLoading(true);
      setRefundErrorMessage("");

      const data = await getRefundContext(paymentId);
      setRefundContext(data);
    } catch (error) {
      setRefundContext(null);
      setRefundErrorMessage(
        error?.body?.message ||
          error?.body?.reason ||
          "refund-context 조회에 실패했습니다."
      );
    } finally {
      setRefundLoading(false);
    }
  }

  function handleMoveToRefundPage() {
    navigate("/merchant/refunds", {
      state: {
        paymentId: paymentDetail?.paymentId,
      },
    });
  }

  if (loading) {
    return (
      <PageLayout
        title="결제 상세"
        description="U3 결제 상세. event 타임라인과 환불 CTA 이동이 들어가는 상세 페이지입니다."
      >
        <div className="guard-notice">
          <div className="guard-notice__title">로딩 중</div>
          <div className="guard-notice__description">
            결제 상세 정보를 불러오는 중입니다.
          </div>
        </div>
      </PageLayout>
    );
  }

  if (errorInfo?.status === 401) {
    return (
      <PageLayout
        title="결제 상세"
        description="U3 결제 상세. event 타임라인과 환불 CTA 이동이 들어가는 상세 페이지입니다."
      >
        <RequireLoginNotice />
      </PageLayout>
    );
  }

  if (errorInfo?.status === 403) {
    return (
      <PageLayout
        title="결제 상세"
        description="U3 결제 상세. event 타임라인과 환불 CTA 이동이 들어가는 상세 페이지입니다."
      >
        <div className="guard-notice">
          <div className="guard-notice__title">접근 불가</div>
          <div className="guard-notice__description">
            {errorInfo.message}
          </div>
        </div>
      </PageLayout>
    );
  }

  if (errorInfo) {
    return (
      <PageLayout
        title="결제 상세"
        description="U3 결제 상세. event 타임라인과 환불 CTA 이동이 들어가는 상세 페이지입니다."
      >
        <div className="guard-notice">
          <div className="guard-notice__title">조회 실패</div>
          <div className="guard-notice__description">
            {errorInfo.message}
          </div>
        </div>
      </PageLayout>
    );
  }

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
                  {paymentDetail?.paymentId}
                </div>
              </div>
            </div>

            <div className="card summary-card payment-summary-card">
              <div className="card__body">
                <div className="summary-card__label">status</div>
                <div className="summary-card__value">
                  <StatusBadge status={paymentDetail?.status} />
                </div>
              </div>
            </div>

            <div className="card summary-card payment-summary-card">
              <div className="card__body">
                <div className="summary-card__label">amount / currency</div>
                <div className="summary-card__value">
                  {formatKrw(paymentDetail?.capturedAmount)} {paymentDetail?.currency}
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
                  <span className="display-field">{paymentDetail?.orderId}</span>
                </div>
              </div>

              <div className="kv-item">
                <div className="kv-item__label">merchantId</div>
                <div className="kv-item__value">
                  <span className="display-field">{paymentDetail?.merchantId}</span>
                </div>
              </div>

              <div className="kv-item">
                <div className="kv-item__label">buyerId</div>
                <div className="kv-item__value">
                  <span className="display-field">{paymentDetail?.buyerId}</span>
                </div>
              </div>

              <div className="kv-item">
                <div className="kv-item__label">requestedAmount</div>
                <div className="kv-item__value">
                  <span className="display-field amount-text">
                    {formatKrw(paymentDetail?.requestedAmount)}
                  </span>
                </div>
              </div>

              <div className="kv-item">
                <div className="kv-item__label">capturedAmount</div>
                <div className="kv-item__value">
                  <span className="display-field amount-text">
                    {formatKrw(paymentDetail?.capturedAmount)}
                  </span>
                </div>
              </div>

              <div className="kv-item">
                <div className="kv-item__label">createdAt</div>
                <div className="kv-item__value">
                  <span className="display-field">
                    {formatDateTimeWithSeconds(paymentDetail?.createdAt)}
                  </span>
                </div>
              </div>

              <div className="kv-item">
                <div className="kv-item__label">capturedAt</div>
                <div className="kv-item__value">
                  <span className="display-field">
                    {formatDateTimeWithSeconds(paymentDetail?.capturedAt)}
                  </span>
                </div>
              </div>

              <div className="kv-item">
                <div className="kv-item__label">confirmed</div>
                <div className="kv-item__value">
                  <span className="display-field">
                    {paymentDetail?.confirmed ? "true" : "false"}
                  </span>
                </div>
              </div>

              <div className="kv-item">
                <div className="kv-item__label">confirmedAt</div>
                <div className="kv-item__value">
                  <span className="display-field">
                    {formatDateTimeWithSeconds(paymentDetail?.confirmedAt)}
                  </span>
                </div>
              </div>
            </div>
          </SectionCard>

          <SectionCard title="환불">
            <div className="page-section">
              <div className="kv-list">
                <div className="kv-item">
                  <div className="kv-item__label">capturedAmount</div>
                  <div className="kv-item__value">
                    <span className="display-field amount-text">
                      {formatKrw(refundContext?.capturedAmount ?? paymentDetail?.capturedAmount)}
                    </span>
                  </div>
                </div>

                <div className="kv-item">
                  <div className="kv-item__label">refundableAmount</div>
                  <div className="kv-item__value">
                    <span className="display-field amount-text">
                      {refundContext
                        ? formatKrw(refundContext?.refundableAmount)
                        : "-"}
                    </span>
                  </div>
                </div>

                <div className="kv-item">
                  <div className="kv-item__label">currency</div>
                  <div className="kv-item__value">
                    <span className="display-field">
                      {refundContext?.currency ?? paymentDetail?.currency}
                    </span>
                  </div>
                </div>
              </div>

              <div className="guard-notice">
                <div className="guard-notice__title">환불 이동</div>
                <div className="guard-notice__description">
                  이 paymentId 기준으로 refund-context 확인 후 환불 요청 화면(U6)으로 이동합니다.
                </div>
              </div>

              {refundErrorMessage && (
                <div className="guard-notice" style={{ marginTop: "12px" }}>
                  <div className="guard-notice__title">refund-context 조회 실패</div>
                  <div className="guard-notice__description">
                    {refundErrorMessage}
                  </div>
                </div>
              )}

              <div className="action-panel">
                <button
                  type="button"
                  className="btn btn--primary"
                  onClick={handleMoveToRefundPage}
                >
                  환불 요청하기
                </button>
                <button
                  type="button"
                  className="btn btn--secondary"
                  onClick={handleLoadRefundContext}
                  disabled={refundLoading}
                >
                  {refundLoading ? "조회 중..." : "refund-context 확인"}
                </button>
              </div>
            </div>
          </SectionCard>
        </div>

        <SectionCard title="결제 이벤트 상세">
          <div className="table-toolbar">
            <div>payment_event 목록</div>
            <div className="action-panel">
              <span className="badge badge--default">insert-only</span>
            </div>
          </div>

          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>occurredAt</th>
                  <th>eventType</th>
                  <th>before</th>
                  <th>after</th>
                </tr>
              </thead>
              <tbody>
                {paymentDetail?.events?.length ? (
                  paymentDetail.events.map((event, index) => (
                    <tr key={`${event.eventType}-${event.occurredAt}-${index}`}>
                      <td>{formatDateTimeWithSeconds(event.occurredAt)}</td>
                      <td>
                        <span className="badge badge--default">{event.eventType}</span>
                      </td>
                      <td>{event.statusBefore ?? "-"}</td>
                      <td>{event.statusAfter ?? "-"}</td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={4}>
                      <div className="empty-state">표시할 이벤트가 없습니다.</div>
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          <div className="guard-notice" style={{ marginTop: "16px" }}>
            <div className="guard-notice__title">표시 규칙</div>
            <div className="guard-notice__description">
              payment.status는 CREATED / CAPTURED만 사용하고, CONFIRMED는
              payment_event의 PAYMENT_CONFIRMED 이벤트로만 표현합니다.
            </div>
          </div>
        </SectionCard>

        <SectionCard title="정책 메모">
          <div className="info-list">
            <div>
              <strong>LOCKED</strong> CONFIRMED는 payment.status가 아니라
              payment_event로만 표현
            </div>
            <div>
              <strong>LOCKED</strong> payment.status는 CREATED / CAPTURED만 사용
            </div>
            <div>
              <strong>연결 화면</strong> U6 환불 요청·현황
            </div>
          </div>
        </SectionCard>
      </div>
    </PageLayout>
  );
}