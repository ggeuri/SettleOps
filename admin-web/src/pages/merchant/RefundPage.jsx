import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import PageLayout from "../../components/layout/PageLayout.jsx";
import SectionCard from "../../components/layout/SectionCard.jsx";
import ActionButton from "../../components/layout/ActionButton.jsx";
import StatusBadge from "../../components/display/StatusBadge.jsx";
import {
    getRefundContext,
    createRefund,
    getMyRefunds,
} from "../../api/refundApi.js";

const INITIAL_FORM = {
    paymentId: "",
    amount: "",
    reasonText: "",
};

export default function RefundPage() {
    const [form, setForm] = useState(INITIAL_FORM);
    const [refundContext, setRefundContext] = useState(null);
    const [refunds, setRefunds] = useState([]);
    const [loadingContext, setLoadingContext] = useState(false);
    const [loadingRefunds, setLoadingRefunds] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [errorMessage, setErrorMessage] = useState("");
    const [successMessage, setSuccessMessage] = useState("");
    const [searchParams] = useSearchParams();

    useEffect(() => {
        loadMyRefunds();
    }, []);

    useEffect(() => {
        const initialPaymentId = searchParams.get("paymentId")?.trim() || "";
        if (!initialPaymentId) return;

        setForm((prev) => ({
            ...prev,
            paymentId: initialPaymentId,
        }));
    }, [searchParams]);

    useEffect(() => {
        const initialPaymentId = searchParams.get("paymentId")?.trim() || "";
        if (!initialPaymentId) return;

        async function bootstrap() {
            setLoadingContext(true);
            setErrorMessage("");
            setSuccessMessage("");
            try {
                const response = await getRefundContext(initialPaymentId);
                setRefundContext(response);
            } catch (error) {
                setRefundContext(null);
                setErrorMessage(
                    error?.body?.message || "refund-context 조회에 실패했습니다."
                );
            } finally {
                setLoadingContext(false);
            }
        }

        bootstrap();
    }, [searchParams]);

    function handleChange(event) {
        const { name, value } = event.target;
        setForm((prev) => ({
            ...prev,
            [name]: value,
        }));
    }

    async function loadRefundContext() {
        const paymentId = form.paymentId.trim();

        if (!paymentId) {
            setErrorMessage("paymentId를 입력해 주세요.");
            setSuccessMessage("");
            setRefundContext(null);
            return;
        }

        setLoadingContext(true);
        setErrorMessage("");
        setSuccessMessage("");

        try {
            const response = await getRefundContext(paymentId);
            setRefundContext(response);
        } catch (error) {
            setRefundContext(null);
            setErrorMessage(
                error?.body?.message || "refund-context 조회에 실패했습니다."
            );
        } finally {
            setLoadingContext(false);
        }
    }

    async function loadMyRefunds() {
        setLoadingRefunds(true);

        try {
            const response = await getMyRefunds();
            setRefunds(Array.isArray(response) ? response : []);
        } catch (error) {
            setErrorMessage(
                error?.body?.message || "내 환불 내역 조회에 실패했습니다."
            );
        } finally {
            setLoadingRefunds(false);
        }
    }

    async function handleSubmit(event) {
        event.preventDefault();

        const paymentId = form.paymentId.trim();
        const reasonText = form.reasonText.trim();
        const rawAmount = form.amount.trim();

        setErrorMessage("");
        setSuccessMessage("");

        if (!paymentId || !reasonText || !rawAmount) {
            setErrorMessage("paymentId, amount, reasonText는 모두 필수입니다.");
            return;
        }

        if (!/^\d+$/.test(rawAmount)) {
            setErrorMessage("amount는 KRW 정수만 입력 가능합니다.");
            return;
        }

        const amount = Number(rawAmount);

        if (!Number.isSafeInteger(amount) || amount <= 0) {
            setErrorMessage("amount는 1 이상의 정수여야 합니다.");
            return;
        }

        if (!refundContext) {
            setErrorMessage("먼저 refund-context를 조회해 주세요.");
            return;
        }

        if (refundContext.currency !== "KRW") {
            setErrorMessage("현재 KRW 환불만 지원합니다.");
            return;
        }

        if (amount > refundContext.refundableAmount) {
            setErrorMessage("refundableAmount를 초과할 수 없습니다.");
            return;
        }

        setSubmitting(true);

        try {
            await createRefund({
                paymentId,
                amount,
                reasonText,
            });

            setSuccessMessage("환불 요청이 등록되었습니다.");
            setForm((prev) => ({
                ...prev,
                amount: "",
                reasonText: "",
            }));
            await loadMyRefunds();
        } catch (error) {
            if (error?.body?.reason) {
                setErrorMessage(`환불 요청 실패: ${error.body.reason}`);
            } else {
                setErrorMessage(
                    error?.body?.message || "환불 요청 등록에 실패했습니다."
                );
            }
        } finally {
            setSubmitting(false);
        }
    }

    return (
        <PageLayout
            title="환불 요청 · 현황"
            description="refund-context를 확인한 뒤 환불 요청을 등록하고, 내 환불 상태를 조회합니다."
        >
            <SectionCard title="환불 요청">
                <form className="form-stack" onSubmit={handleSubmit}>
                    <div className="form-field">
                        <label className="form-field__label">paymentId</label>
                        <input
                            className="input"
                            name="paymentId"
                            value={form.paymentId}
                            onChange={handleChange}
                            placeholder="결제 ID 입력"
                        />
                    </div>

                    <div className="button-row">
                        <ActionButton
                            type="button"
                            variant="secondary"
                            disabled={loadingContext}
                            onClick={loadRefundContext}
                        >
                            {loadingContext ? "조회 중..." : "refund-context 조회"}
                        </ActionButton>
                    </div>

                    {refundContext ? (
                        <div className="info-list">
                            <div>
                                <strong>paymentId</strong>{" "}
                                <span className="copyable-id">
                  <span className="copyable-id__text">
                    {refundContext.paymentId}
                  </span>
                </span>
                            </div>

                            <div>
                                <strong>status</strong>{" "}
                                <StatusBadge status={refundContext.status} />
                            </div>

                            <div>
                                <strong>capturedAmount</strong> {refundContext.capturedAmount}
                            </div>

                            <div>
                                <strong>refundableAmount</strong>{" "}
                                {refundContext.refundableAmount}
                            </div>

                            <div>
                                <strong>currency</strong> {refundContext.currency}
                            </div>

                            <div>
                                <strong>merchantId</strong> {refundContext.merchantId}
                            </div>

                            <div>
                                <strong>capturedAt</strong> {refundContext.capturedAt || "-"}
                            </div>
                        </div>
                    ) : null}

                    <div className="form-field">
                        <label className="form-field__label">amount</label>
                        <input
                            className="input"
                            type="text"
                            name="amount"
                            value={form.amount}
                            onChange={handleChange}
                            placeholder="환불 금액"
                            inputMode="numeric"
                        />
                    </div>

                    <div className="form-field">
                        <label className="form-field__label">reasonText</label>
                        <textarea
                            className="textarea"
                            name="reasonText"
                            value={form.reasonText}
                            onChange={handleChange}
                            placeholder="환불 사유 입력"
                        />
                    </div>

                    <div className="button-row">
                        <ActionButton type="submit" disabled={submitting}>
                            {submitting ? "요청 중..." : "환불 요청"}
                        </ActionButton>
                    </div>
                </form>
            </SectionCard>

            {errorMessage ? <div className="state-error">{errorMessage}</div> : null}

            {successMessage ? (
                <div className="state-success">{successMessage}</div>
            ) : null}

            <SectionCard title="내 환불 현황">
                {loadingRefunds ? (
                    <div className="state-block">
                        <div className="state-block__title">로딩 중</div>
                        <div className="state-block__description">
                            환불 내역을 불러오고 있습니다.
                        </div>
                    </div>
                ) : refunds.length === 0 ? (
                    <div className="state-block">
                        <div className="state-block__title">환불 내역 없음</div>
                        <div className="state-block__description">
                            현재 조회되는 환불 요청이 없습니다.
                        </div>
                    </div>
                ) : (
                    <div className="table-wrap">
                        <table className="data-table">
                            <thead>
                            <tr>
                                <th>refundId</th>
                                <th>paymentId</th>
                                <th>amount</th>
                                <th>status</th>
                                <th>requestedAt</th>
                                <th>decidedAt</th>
                            </tr>
                            </thead>
                            <tbody>
                            {refunds.map((row) => (
                                <tr key={row.refundId}>
                                    <td>
                      <span className="copyable-id">
                        <span className="copyable-id__text copyable-id__text--short">
                          {row.refundId}
                        </span>
                      </span>
                                    </td>

                                    <td>
                      <span className="copyable-id">
                        <span className="copyable-id__text copyable-id__text--short">
                          {row.paymentId}
                        </span>
                      </span>
                                    </td>

                                    <td>{row.amount}</td>

                                    <td>
                                        <StatusBadge status={row.status} />
                                    </td>

                                    <td>{row.requestedAt || "-"}</td>
                                    <td>{row.decidedAt || "-"}</td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </SectionCard>
        </PageLayout>
    );
}