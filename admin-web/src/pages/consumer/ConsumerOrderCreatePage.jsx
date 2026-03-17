import { useState } from "react";
import { createConsumerOrder } from "../../api/consumerOrders.js";
import "./ConsumerOrderCreatePage.css";

export default function ConsumerOrderCreatePage() {
    const [merchantId, setMerchantId] = useState("");
    const [buyerId, setBuyerId] = useState("");
    const [itemName, setItemName] = useState("");
    const [amount, setAmount] = useState("");

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [result, setResult] = useState(null);

    async function handleSubmit(e) {
        e.preventDefault();

        setError("");

        if (!merchantId || !buyerId || !itemName || !amount) {
            setError("모든 값을 입력하세요");
            return;
        }

        if (Number(amount) <= 0) {
            setError("amount > 0");
            return;
        }

        setLoading(true);

        try {
            const res = await createConsumerOrder({
                merchantId,
                buyerId,
                itemName,
                amount: Number(amount),
            });

            setResult(res);
        } catch (e) {
            setError("주문 생성 실패");
        } finally {
            setLoading(false);
        }
    }

    return (
        <div className="page">
            <h1>C1 주문 생성</h1>

            <form onSubmit={handleSubmit}>

                <input
                    placeholder="merchantId"
                    value={merchantId}
                    onChange={(e) => setMerchantId(e.target.value)}
                />

                <input
                    placeholder="buyerId"
                    value={buyerId}
                    onChange={(e) => setBuyerId(e.target.value)}
                />

                <input
                    placeholder="itemName"
                    value={itemName}
                    onChange={(e) => setItemName(e.target.value)}
                />

                <input
                    type="number"
                    placeholder="amount"
                    value={amount}
                    onChange={(e) => setAmount(e.target.value)}
                />

                <button disabled={loading}>
                    {loading ? "생성중" : "주문 생성"}
                </button>

            </form>

            {error && <p>{error}</p>}

            {result && (
                <div>
                    <p>orderId: {result.orderId}</p>
                    <p>status: {result.status}</p>
                    <p>amount: {result.amount}</p>
                </div>
            )}
        </div>
    );
}