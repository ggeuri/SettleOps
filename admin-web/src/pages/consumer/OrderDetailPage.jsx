import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { getConsumerOrderDetail } from "../../api/consumerOrderApi.js";
import { payOrder, confirmPayment } from "../../api/paymentApi.js";
import PageLayout from "../../components/layout/PageLayout.jsx";
import SectionCard from "../../components/layout/SectionCard.jsx";
import ActionButton from "../../components/layout/ActionButton.jsx";
import StatusBadge from "../../components/display/StatusBadge.jsx";
import RequireLoginNotice from "../../components/common/RequireLoginNotice.jsx";

export default function OrderDetailPage() {
  const { orderId } = useParams();

  const [orderDetail, setOrderDetail] = useState(null);
  const [loading, setLoading] = useState(true);
  const [errorInfo, setErrorInfo] = useState(null);

  const [idempotencyKey, setIdempotencyKey] = useState("idem_20260318_demo_key");
  const [submitting, setSubmitting] = useState(false);
  const [actionInfo, setActionInfo] = useState(null);

  async function loadOrderDetail() {
    try {
      setLoading(true);
      setErrorInfo(null);

      const data = await getConsumerOrderDetail(orderId);
      setOrderDetail(data);
//       console.log(data);
    } catch (error) {
      setOrderDetail(null);
      setErrorInfo({
        status: error?.status ?? null,
        message:
          error?.body?.message ||
          error?.body?.reason ||
          "주문 정보를 불러오지 못했습니다.",
      });
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (orderId) {
      loadOrderDetail();
    }
  }, [orderId]);

  const orderStatus = orderDetail?.orderStatus ?? null;
  const paymentStatus = orderDetail?.paymentStatus ?? null;
  const paymentId = orderDetail?.paymentId ?? null;
  const isConfirmed =
    orderDetail?.confirmed === true || Boolean(orderDetail?.confirmedAt);

  const canPay = orderStatus === "CREATED";

  const canConfirm =
    orderStatus === "PAID" &&
    paymentStatus === "CAPTURED" &&
    Boolean(paymentId) &&
    !isConfirmed;

  const isConfirmDone = isConfirmed;

  async function handlePay() {
    const trimmedKey = idempotencyKey.trim();

    if (!trimmedKey) {
      setActionInfo({
        type: "error",
        message: "X-Idempotency-Key를 입력해주세요.",
      });
      return;
    }

    try {
      setSubmitting(true);
      setActionInfo(null);

      const response = await payOrder(orderId, trimmedKey);

      setActionInfo({
        type: "success",
        message: `결제 승인이 완료되었습니다. status=${response?.status ?? "CAPTURED"}`,
      });

      await loadOrderDetail();
    } catch (error) {
      setActionInfo({
        type: "error",
        message:
          error?.body?.message ||
          error?.body?.reason ||
          "결제 승인에 실패했습니다.",
      });
    } finally {
      setSubmitting(false);
    }
  }

  async function handleConfirm() {
    if (!paymentId) {
      setActionInfo({
        type: "error",
        message: "구매확정 대상 paymentId가 없습니다.",
      });
      return;
    }

    try {
      setSubmitting(true);
      setActionInfo(null);

      const response = await confirmPayment(paymentId);

      setActionInfo({
        type: "success",
        message: `구매확정이 완료되었습니다. confirmedAt=${response?.confirmedAt ?? "-"}`,
      });

      await loadOrderDetail();
    } catch (error) {
      setActionInfo({
        type: "error",
        message:
          error?.body?.message ||
          error?.body?.reason ||
          "구매확정에 실패했습니다.",
      });
    } finally {
      setSubmitting(false);
    }
  }

  async function handleCopyKey() {
    try {
      await navigator.clipboard.writeText(idempotencyKey);
      setActionInfo({
        type: "success",
        message: "멱등 키를 복사했습니다.",
      });
    } catch {
      setActionInfo({
        type: "error",
        message: "멱등 키 복사에 실패했습니다.",
      });
    }
  }

  if (loading) {
    return (
      <PageLayout
        title="결제 상세"
        description="C2 결제 상세(승인 버튼). orderId 기준 결제 승인과 멱등 UX가 들어갈 페이지입니다."
      >
        <div className="guard-notice">
          <div className="guard-notice__title">로딩 중</div>
          <div className="guard-notice__description">
            주문 / 결제 정보를 불러오는 중입니다.
          </div>
        </div>
      </PageLayout>
    );
  }

  if (errorInfo?.status === 401) {
    return (
      <PageLayout
        title="결제 상세"
        description="C2 결제 상세(승인 버튼). orderId 기준 결제 승인과 멱등 UX가 들어갈 페이지입니다."
      >
        <RequireLoginNotice />
      </PageLayout>
    );
  }

  if (errorInfo?.status === 403) {
    return (
      <PageLayout
        title="결제 상세"
        description="C2 결제 상세(승인 버튼). orderId 기준 결제 승인과 멱등 UX가 들어갈 페이지입니다."
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
        description="C2 결제 상세(승인 버튼). orderId 기준 결제 승인과 멱등 UX가 들어갈 페이지입니다."
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
      description="C2 결제 상세(승인 버튼). orderId 기준 결제 승인과 멱등 UX가 들어갈 페이지입니다."
    >
      <SectionCard title="주문 / 결제 요약">
        <div className="kv-list">
          <div className="kv-item">
            <div className="kv-item__label">orderId</div>
            <div className="kv-item__value">
              <span className="display-field">{orderDetail?.orderId}</span>
            </div>
          </div>

          <div className="kv-item">
            <div className="kv-item__label">orderStatus</div>
            <div className="kv-item__value">
              <StatusBadge status={orderDetail?.orderStatus} />
            </div>
          </div>

          <div className="kv-item">
            <div className="kv-item__label">paymentId</div>
            <div className="kv-item__value">
              <span className="display-field copyable-id__text">
                {orderDetail?.paymentId ?? "-"}
              </span>
            </div>
          </div>

          <div className="kv-item">
            <div className="kv-item__label">paymentStatus</div>
            <div className="kv-item__value">
              <StatusBadge status={orderDetail?.paymentStatus} />
            </div>
          </div>

          <div className="kv-item">
            <div className="kv-item__label">confirmed</div>
            <div className="kv-item__value">
              <span className="display-field">
                {isConfirmed ? "true" : "false"}
              </span>
            </div>
          </div>

          <div className="kv-item">
            <div className="kv-item__label">confirmedAt</div>
            <div className="kv-item__value">
              <span className="display-field">
                {orderDetail?.confirmedAt ?? "-"}
              </span>
            </div>
          </div>

          <div className="kv-item">
            <div className="kv-item__label">merchantId</div>
            <div className="kv-item__value">
              <span className="display-field">{orderDetail?.merchantId}</span>
            </div>
          </div>

          <div className="kv-item">
            <div className="kv-item__label">buyerId</div>
            <div className="kv-item__value">
              <span className="display-field">{orderDetail?.buyerId}</span>
            </div>
          </div>

          <div className="kv-item">
            <div className="kv-item__label">itemName</div>
            <div className="kv-item__value">
              <span className="display-field">{orderDetail?.itemName}</span>
            </div>
          </div>

          <div className="kv-item">
            <div className="kv-item__label">amount</div>
            <div className="kv-item__value">
              <span className="display-field amount-text">
                {orderDetail?.amount?.toLocaleString()}원
              </span>
            </div>
          </div>
        </div>
      </SectionCard>

      <SectionCard title="결제 액션">
        <div className="form-stack">
          <div className="form-field">
            <label className="form-field__label">X-Idempotency-Key</label>
            <input
              className="input"
              placeholder="멱등 키 입력"
              value={idempotencyKey}
              onChange={(e) => setIdempotencyKey(e.target.value)}
              disabled={!canPay || submitting}
            />
          </div>

          <div className="guard-notice">
            <div className="guard-notice__title">멱등 처리 안내</div>
            <div className="guard-notice__description">
              동일 orderId + X-Idempotency-Key 재요청은 no-op 200으로 현재 결과를 반환할 수 있습니다.
              성공 저장 전 경합 구간에서는 409 IN_PROGRESS가 반환될 수 있습니다.
            </div>
          </div>

          {actionInfo && (
            <div className="guard-notice">
              <div className="guard-notice__title">
                {actionInfo.type === "success" ? "처리 완료" : "처리 실패"}
              </div>
              <div className="guard-notice__description">
                {actionInfo.message}
              </div>
            </div>
          )}

          <div className="button-row action-panel">
            {canPay && (
              <ActionButton
                type="button"
                onClick={handlePay}
                disabled={submitting}
              >
                {submitting ? "처리 중..." : "결제 승인"}
              </ActionButton>
            )}

            {canConfirm && (
              <ActionButton
                type="button"
                onClick={handleConfirm}
                disabled={submitting}
              >
                {submitting ? "처리 중..." : "구매확정"}
              </ActionButton>
            )}

            {isConfirmDone && (
              <button type="button" className="btn btn--secondary" disabled>
                구매확정 완료
              </button>
            )}

            <button
              type="button"
              className="btn btn--secondary"
              onClick={handleCopyKey}
              disabled={submitting}
            >
              멱등 키 복사
            </button>
          </div>
        </div>
      </SectionCard>
    </PageLayout>
  );
}