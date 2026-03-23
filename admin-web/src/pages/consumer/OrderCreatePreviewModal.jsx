import ActionButton from "../../components/layout/ActionButton.jsx";
import InfoRow from "../../components/display/InfoRow.jsx";
import AmountText from "../../components/display/AmountText.jsx";

export default function OrderCreatePreviewModal({
                                                    open,
                                                    form,
                                                    submitting,
                                                    onClose,
                                                    onConfirm,
                                                }) {
    if (!open) {
        return null;
    }

    function handleBackdropClick(e) {
        if (e.target === e.currentTarget) {
            onClose?.();
        }
    }

    return (
        <div className="app-modal-backdrop" onClick={handleBackdropClick}>
            <div className="app-modal">

                <div className="app-modal__header">
                    <h2 className="app-modal__title">
                        주문신청 내역 확인
                    </h2>

                    <button
                        type="button"
                        className="app-modal__close"
                        onClick={onClose}
                        disabled={submitting}
                    >
                        닫기
                    </button>
                </div>

                <div className="app-modal__body">

                    <div className="form-stack">

                        <div className="guard-notice">
                            <div className="guard-notice__title">
                                미리보기
                            </div>

                            <div className="guard-notice__description">
                                아래 내용으로 주문신청(seed 생성)하시겠습니까?
                            </div>
                        </div>


                        <div className="kv-list">

                            <InfoRow label="merchantId">
                                <span className="display-field">
                                    {form.merchantId || "-"}
                                </span>
                            </InfoRow>

                            <InfoRow label="상품/주문명">
                                <span className="display-field">
                                    {form.itemName || "-"}
                                </span>
                            </InfoRow>

                            <InfoRow label="amount">
                                <AmountText
                                    value={
                                        form.amount
                                            ? Number(form.amount)
                                            : ""
                                    }
                                />
                            </InfoRow>

                            <InfoRow label="buyerId">
                                <span className="display-field">
                                    {form.buyerId || "-"}
                                </span>
                            </InfoRow>

                        </div>


                        <div className="guard-notice">
                            <div className="guard-notice__description">
                                주의: 본 화면은 비멱등 seed이며,
                                중복 생성이 허용됩니다.
                                실제 결제 승인/상태 변경은
                                다음 화면에서 수행됩니다.
                            </div>
                        </div>

                    </div>

                </div>


                <div className="app-modal__footer">

                    <div className="button-row">

                        <ActionButton
                            type="button"
                            variant="secondary"
                            onClick={onClose}
                            disabled={submitting}
                        >
                            수정
                        </ActionButton>

                        <ActionButton
                            type="button"
                            onClick={onConfirm}
                            disabled={submitting}
                        >
                            {submitting
                                ? "생성 중..."
                                : "맞습니다. 주문신청"}
                        </ActionButton>

                    </div>

                </div>

            </div>
        </div>
    );
}