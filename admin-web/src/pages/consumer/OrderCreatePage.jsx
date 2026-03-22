import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import PageLayout from "../../components/layout/PageLayout.jsx";
import SectionCard from "../../components/layout/SectionCard.jsx";
import ActionButton from "../../components/layout/ActionButton.jsx";
import { createConsumerOrder } from "../../api/consumerOrderApi.js";
import OrderCreatePreviewModal from "./OrderCreatePreviewModal.jsx";
import { getMe } from "../../api/meApi.js";

const INITIAL_FORM = {
    merchantId: "",
    buyerId: "",
    itemName: "",
    amount: "",
};

export default function OrderCreatePage() {
    const [form, setForm] = useState(INITIAL_FORM);
    const [loading, setLoading] = useState(false);
    const [errorMessage, setErrorMessage] = useState("");
    const [previewOpen, setPreviewOpen] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [meLoading, setMeLoading] = useState(true);

    const navigate = useNavigate();

    function handleChange(event) {
        const { name, value } = event.target;

        setForm((prev) => ({
            ...prev,
            [name]: value,
        }));

        setErrorMessage("");
    }

    function validateForm() {
        const merchantId = form.merchantId.trim();
        const itemName = form.itemName.trim();
        const rawAmount = form.amount.trim();

        if (!form.buyerId.trim()) {
            return "로그인 사용자 정보를 먼저 확인해야 합니다.";
        }

        if (!merchantId || !itemName || !rawAmount) {
            return "merchantId, 상품/주문명, amount를 입력해야 합니다.";
        }

        if (!/^\d+$/.test(rawAmount)) {
            return "amount는 정수만 입력 가능합니다.";
        }

        const amount = Number(rawAmount);
        if (!Number.isSafeInteger(amount) || amount <= 0) {
            return "amount는 1 이상의 정수여야 합니다.";
        }

        return "";
    }

    function handleSubmit(event) {
        event.preventDefault();

        const validationMessage = validateForm();
        if (validationMessage) {
            setErrorMessage(validationMessage);
            return;
        }

        setErrorMessage("");
        setPreviewOpen(true);
    }

    function handleClosePreview() {
        if (submitting) {
            return;
        }

        setPreviewOpen(false);
    }

    async function handleConfirmCreate() {
        const merchantId = form.merchantId.trim();
        const itemName = form.itemName.trim();
        const amount = Number(form.amount.trim());

        setSubmitting(true);
        setLoading(true);
        setErrorMessage("");

        try {
            const response = await createConsumerOrder({
                merchantId,
                itemName,
                amount,
            });

            setPreviewOpen(false);
            setForm((prev) => ({
                ...INITIAL_FORM,
                buyerId: prev.buyerId,
            }));
            navigate(`/consumer/orders/${response.orderId}`);
        } catch (error) {
            setPreviewOpen(false);
            setErrorMessage(
                error?.body?.message ||
                error?.body?.reason ||
                "주문 생성에 실패했습니다."
            );
        } finally {
            setSubmitting(false);
            setLoading(false);
        }
    }

    useEffect(() => {
        let cancelled = false;

        async function fetchMe() {
            try {
                const response = await getMe();

                if (cancelled) {
                    return;
                }

                setForm((prev) => ({
                    ...prev,
                    buyerId: response.buyerId ?? "",
                }));

                if (!response.buyerId) {
                    setErrorMessage("로그인 사용자 식별자(buyerId)를 확인할 수 없습니다.");
                }
            } catch (error) {
                if (cancelled) {
                    return;
                }

                setErrorMessage("로그인 사용자 정보를 불러오지 못했습니다.");
            } finally {
                if (!cancelled) {
                    setMeLoading(false);
                }
            }
        }

        fetchMe();

        return () => {
            cancelled = true;
        };
    }, []);

    return (
        <PageLayout
            title="거래 생성"
            description="seed 거래 최소 스펙만 입력합니다."
        >
            <SectionCard title="주문 생성">
                <form className="form-stack" onSubmit={handleSubmit}>
                    <div className="form-field">
                        <label className="form-field__label">merchantId</label>
                        <input
                            className="input"
                            name="merchantId"
                            value={form.merchantId}
                            onChange={handleChange}
                            placeholder="MERCHANT_1001"
                        />
                    </div>

                    <div className="form-field">
                        <label className="form-field__label">buyerId</label>
                        <input
                            className="input"
                            name="buyerId"
                            value={form.buyerId}
                            readOnly
                        />
                    </div>

                    <div className="form-field">
                        <label className="form-field__label">상품/주문명</label>
                        <input
                            className="input"
                            name="itemName"
                            value={form.itemName}
                            onChange={handleChange}
                            placeholder="쿠쿠 밥솥 20년 모델"
                        />
                    </div>

                    <div className="form-field">
                        <label className="form-field__label">amount</label>
                        <input
                            className="input"
                            name="amount"
                            type="number"
                            step="1"
                            min="1"
                            inputMode="numeric"
                            value={form.amount}
                            onChange={handleChange}
                            placeholder="15000"
                        />
                    </div>

                    {errorMessage ? (
                        <div className="state-error">{errorMessage}</div>
                    ) : null}

                    <div className="button-row">
                        <ActionButton type="submit" disabled={loading || submitting || meLoading || !form.buyerId}>
                            {loading ? "생성 중..." : "주문 생성"}
                        </ActionButton>
                    </div>
                </form>
            </SectionCard>

            <OrderCreatePreviewModal
                open={previewOpen}
                form={form}
                submitting={submitting}
                onClose={handleClosePreview}
                onConfirm={handleConfirmCreate}
            />
        </PageLayout>
    );
}