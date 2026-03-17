import { useState } from "react";
import PageLayout from "../../components/layout/PageLayout.jsx";
import SectionCard from "../../components/layout/SectionCard.jsx";
import ActionButton from "../../components/layout/ActionButton.jsx";
import StatusBadge from "../../components/display/StatusBadge.jsx";
import { createConsumerOrder } from "../../api/consumerOrderApi.js";

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
    const [result, setResult] = useState(null);

    function handleChange(event) {
        const { name, value } = event.target;

        setForm((prev) => ({
            ...prev,
            [name]: value,
        }));

        setErrorMessage("");
        setResult(null);
    }

    async function handleSubmit(e) {
        e.preventDefault();
        setErrorMessage("");
        setResult(null);

        const merchantId = form.merchantId.trim();
        const buyerId = form.buyerId.trim();
        const itemName = form.itemName.trim();
        const rawAmount = form.amount.trim();

        if (!merchantId || !buyerId || !itemName || !rawAmount) {
            setErrorMessage("모든 값을 입력해야 합니다.");
            return;
        }

        if (!/^\d+$/.test(rawAmount)) {
            setErrorMessage("amount는 KRW 정수(원)만 입력 가능합니다.");
            return;
        }

        const amount = Number(rawAmount);

        if (!Number.isSafeInteger(amount) || amount <= 0) {
            setErrorMessage("amount는 1원 이상의 정수여야 합니다.");
            return;
        }

        setLoading(true);

        try {
            const response = await createConsumerOrder({
                merchantId,
                buyerId,
                itemName,
                amount,
            });

            setResult(response);
            setForm(INITIAL_FORM);
        } catch (error) {
            setErrorMessage(error?.body?.message || "주문 생성에 실패했습니다.");
        } finally {
            setLoading(false);
        }
    }

    return (
        <PageLayout
            title="거래 생성"
            description="seed 거래 최소 스펙만 입력합니다."
        >
            <SectionCard>
                <form className="form-stack" onSubmit={handleSubmit}>
                    <div className="form-field">
                        <label>merchantId</label>
                        <input
                            name="merchantId"
                            value={form.merchantId}
                            onChange={handleChange}
                        />
                    </div>

                    <div className="form-field">
                        <label>buyerId</label>
                        <input
                            name="buyerId"
                            value={form.buyerId}
                            onChange={handleChange}
                        />
                    </div>

                    <div className="form-field">
                        <label>itemName</label>
                        <input
                            name="itemName"
                            value={form.itemName}
                            onChange={handleChange}
                        />
                    </div>

                    <div className="form-field">
                        <label>amount</label>
                        <input
                            name="amount"
                            type="number"
                            step="1"
                            min="1"
                            inputMode="numeric"
                            value={form.amount}
                            onChange={handleChange}
                        />
                    </div>

                    {errorMessage ? <div className="state-error">{errorMessage}</div> : null}

                    <div className="button-row">
                        <ActionButton type="submit" disabled={loading}>
                            {loading ? "생성 중..." : "주문 생성"}
                        </ActionButton>
                    </div>
                </form>
            </SectionCard>

            {result ? (
                <SectionCard>
                    <div className="info-list">
                        <div><strong>orderId</strong> {result.orderId}</div>
                        <div><strong>shortId</strong> {result.orderId?.slice(0, 8)}</div>
                        <div><strong>status</strong> <StatusBadge status={result.status} /></div>
                        <div><strong>amount</strong> {result.amount}</div>
                    </div>
                </SectionCard>
            ) : null}
        </PageLayout>
    );
}