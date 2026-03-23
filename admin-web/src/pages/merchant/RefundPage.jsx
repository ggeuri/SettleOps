import { useEffect, useMemo, useState } from "react";
import { useLocation, useSearchParams } from "react-router-dom";
import PageLayout from "../../components/layout/PageLayout.jsx";
import SectionCard from "../../components/layout/SectionCard.jsx";
import ActionButton from "../../components/layout/ActionButton.jsx";
import StatusBadge from "../../components/display/StatusBadge.jsx";
import Pagination from "../../components/table/Pagination.jsx";
import {
    getRefundContext,
    createRefund,
    getMyRefunds,
} from "../../api/refundApi.js";
import {
    formatDateTime,
    formatDateTimeWithSeconds,
} from "../../utils/format.js";

const INITIAL_FORM = {
    paymentId: "",
    amount: "",
    reasonText: "",
};

const INITIAL_FILTER = {
    from: "",
    to: "",
    status: "ALL",
    keyword: "",
};

const INITIAL_SORT = {
    key: "requestedAt",
    direction: "desc",
};

function formatShortId(value, head = 8, tail = 4) {
    if (!value) return "-";
    if (value.length <= head + tail + 3) return value;
    return `${value.slice(0, head)}...${value.slice(-tail)}`;
}

function formatAmount(value) {
    if (value === null || value === undefined || value === "") return "-";
    return Number(value).toLocaleString("ko-KR");
}

function getDerivedLabel(row) {
    if (
        row.status === "APPROVED" &&
        row.capturedAmount != null &&
        Number(row.amount) === Number(row.capturedAmount)
    ) {
        return "전액 환불(파생)";
    }

    return "-";
}

function compareValues(a, b) {
    if (a === b) return 0;
    if (a === null || a === undefined || a === "") return 1;
    if (b === null || b === undefined || b === "") return -1;

    const aNumber = Number(a);
    const bNumber = Number(b);

    if (!Number.isNaN(aNumber) && !Number.isNaN(bNumber)) {
        return aNumber - bNumber;
    }

    return String(a).localeCompare(String(b), "ko-KR", {
        numeric: true,
        sensitivity: "base",
    });
}

function SortHeader({ label, columnKey, sort, onSortChange }) {
    const isActive = sort.key === columnKey;
    const arrow = !isActive ? "⇅" : sort.direction === "asc" ? "▲" : "▼";

    function handleClick() {
        if (isActive) {
            onSortChange({
                key: columnKey,
                direction: sort.direction === "asc" ? "desc" : "asc",
            });
            return;
        }

        onSortChange({
            key: columnKey,
            direction: "asc",
        });
    }

    return (
        <th style={{ padding: "12px 18px", lineHeight: 1.2 }}>
            <button
                type="button"
                onClick={handleClick}
                style={{
                    display: "inline-flex",
                    alignItems: "center",
                    gap: "6px",
                    border: "none",
                    background: "transparent",
                    padding: 0,
                    margin: 0,
                    font: "inherit",
                    fontWeight: 700,
                    color: isActive ? "#111827" : "#667085",
                    cursor: "pointer",
                }}
            >
                <span>{label}</span>
                <span
                    style={{
                        fontSize: "11px",
                        lineHeight: 1,
                        color: isActive ? "#111827" : "#98a2b3",
                    }}
                >
                    {arrow}
                </span>
            </button>
        </th>
    );
}

function ContextRow({ label, value, isStatus = false }) {
    const displayValue =
        value === null || value === undefined || value === "" ? "-" : value;

    return (
        <div
            style={{
                display: "grid",
                gridTemplateColumns: "140px 1fr",
                gap: "8px",
                alignItems: "center",
                padding: "6px 0",
                borderTop: "1px solid #eef1f4",
                fontSize: "13px",
            }}
        >
            <span style={{ color: "#667085", fontWeight: 600 }}>
                {label}
            </span>

            <span
                style={{
                    textAlign: "right",
                    whiteSpace: "nowrap",
                    overflow: "hidden",
                    textOverflow: "ellipsis",
                }}
                title={displayValue}
            >
                {isStatus && displayValue !== "-" ? (
                    <StatusBadge status={displayValue} />
                ) : (
                    displayValue
                )}
            </span>
        </div>
    );
}

export default function RefundPage() {
    const [form, setForm] = useState(INITIAL_FORM);
    const [filter, setFilter] = useState(INITIAL_FILTER);
    const [sort, setSort] = useState(INITIAL_SORT);
    const [refundContext, setRefundContext] = useState(null);
    const [refunds, setRefunds] = useState([]);
    const [loadingContext, setLoadingContext] = useState(false);
    const [loadingRefunds, setLoadingRefunds] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [errorMessage, setErrorMessage] = useState("");
    const [successMessage, setSuccessMessage] = useState("");
    const [searchParams] = useSearchParams();
    const location = useLocation();
    const [page, setPage] = useState(0);
    const pageSize = 7;

    const initialPaymentId =
        searchParams.get("paymentId")?.trim() ||
        location.state?.paymentId?.trim?.() ||
        "";

    useEffect(() => {
        loadMyRefunds();
    }, []);

    useEffect(() => {
        if (!initialPaymentId) return;

        setForm((prev) => ({
            ...prev,
            paymentId: initialPaymentId,
        }));
    }, [initialPaymentId]);

    useEffect(() => {
        setPage(0);
    }, [filter, sort]);

    function handleChange(event) {
        const { name, value } = event.target;
        setForm((prev) => ({
            ...prev,
            [name]: value,
        }));
    }

    function handleFilterChange(event) {
        const { name, value } = event.target;
        setFilter((prev) => ({
            ...prev,
            [name]: value,
        }));
    }

    function handleFilterReset() {
        setFilter(INITIAL_FILTER);
    }

    async function handleLoadContext() {
        const paymentId = form.paymentId.trim();

        if (!paymentId) {
            setRefundContext(null);
            setErrorMessage("paymentId를 입력해주세요.");
            setSuccessMessage("");
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

    const filteredRefunds = useMemo(() => {
        return refunds.filter((row) => {
            const rowStatus = row.status || "";
            const rowRequestedAt = row.requestedAt || "";
            const keyword = filter.keyword.trim().toLowerCase();

            const matchesStatus =
                filter.status === "ALL" || rowStatus === filter.status;

            const requestedDate = rowRequestedAt ? rowRequestedAt.slice(0, 10) : "";

            const matchesFrom =
                !filter.from || (requestedDate && requestedDate >= filter.from);

            const matchesTo =
                !filter.to || (requestedDate && requestedDate <= filter.to);

            const matchesKeyword =
                !keyword ||
                [row.refundId, row.paymentId, row.orderId]
                    .filter(Boolean)
                    .some((value) =>
                        String(value).toLowerCase().includes(keyword)
                    );

            return matchesStatus && matchesFrom && matchesTo && matchesKeyword;
        });
    }, [refunds, filter]);

    const sortedRefunds = useMemo(() => {
        const copied = [...filteredRefunds];

        copied.sort((a, b) => {
            let leftValue;
            let rightValue;

            switch (sort.key) {
                case "refundId":
                    leftValue = a.refundId;
                    rightValue = b.refundId;
                    break;
                case "paymentId":
                    leftValue = a.paymentId;
                    rightValue = b.paymentId;
                    break;
                case "amount":
                    leftValue = Number(a.amount ?? 0);
                    rightValue = Number(b.amount ?? 0);
                    break;
                case "status":
                    leftValue = a.status;
                    rightValue = b.status;
                    break;
                case "derived":
                    leftValue = getDerivedLabel(a);
                    rightValue = getDerivedLabel(b);
                    break;
                case "requestedAt":
                    leftValue = a.requestedAt;
                    rightValue = b.requestedAt;
                    break;
                case "decidedAt":
                    leftValue = a.decidedAt;
                    rightValue = b.decidedAt;
                    break;
                default:
                    leftValue = a.requestedAt;
                    rightValue = b.requestedAt;
                    break;
            }

            const compared = compareValues(leftValue, rightValue);
            return sort.direction === "asc" ? compared : compared * -1;
        });

        return copied;
    }, [filteredRefunds, sort]);

    const totalPages = Math.max(1, Math.ceil(sortedRefunds.length / pageSize));

    const pagedRefunds = useMemo(() => {
        const start = page * pageSize;
        return sortedRefunds.slice(start, start + pageSize);
    }, [sortedRefunds, page]);

    useEffect(() => {
        if (page > totalPages - 1) {
            setPage(0);
        }
    }, [page, totalPages]);

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
            description="상단에서 환불 요청을 등록하고, 하단에서 내 환불 현황을 조회합니다."
        >
            <SectionCard title="환불 요청">
                <div
                    style={{
                        display: "grid",
                        gridTemplateColumns: "minmax(0, 1.3fr) minmax(380px, 1fr)",
                        gap: "20px",
                        alignItems: "start",
                    }}
                >
                    <div>
                        <div className="form-field">
                            <label className="form-field__label">paymentId</label>

                            <div
                                style={{
                                    display: "flex",
                                    gap: "8px",
                                    alignItems: "center",
                                }}
                            >
                                <input
                                    className="input"
                                    name="paymentId"
                                    value={form.paymentId}
                                    onChange={handleChange}
                                    placeholder="결제 ID 입력"
                                    style={{
                                        flex: 1,
                                        padding: "12px 14px",
                                        lineHeight: 1.4,
                                    }}
                                />

                                <ActionButton
                                    type="button"
                                    variant="secondary"
                                    onClick={handleLoadContext}
                                    disabled={loadingContext}
                                >
                                    {loadingContext ? "조회 중..." : "조회"}
                                </ActionButton>
                            </div>
                        </div>

                        <div className="form-field" style={{ marginTop: "8px" }}>
                            <label className="form-field__label">amount</label>
                            <input
                                className="input"
                                name="amount"
                                value={form.amount}
                                onChange={handleChange}
                                placeholder="환불 금액"
                                style={{
                                    padding: "12px 14px",
                                    lineHeight: 1.4,
                                }}
                            />
                        </div>

                        <div className="form-field" style={{ marginTop: "8px" }}>
                            <label className="form-field__label">reasonText</label>
                            <textarea
                                className="input"
                                name="reasonText"
                                value={form.reasonText}
                                onChange={handleChange}
                                placeholder="환불 사유 입력"
                                rows={2}
                                style={{
                                    padding: "12px 14px",
                                    lineHeight: 1.5,
                                    resize: "vertical",
                                }}
                            />
                        </div>

                        <div className="button-row" style={{ marginTop: "10px" }}>
                            <ActionButton
                                type="button"
                                onClick={handleSubmit}
                                disabled={submitting}
                            >
                                {submitting ? "요청 중..." : "환불 요청"}
                            </ActionButton>
                        </div>
                    </div>

                    <div style={{ alignSelf: "start" }}>
                        <div
                            style={{
                                border: "1px solid var(--color-border, #d9dde3)",
                                borderRadius: "14px",
                                padding: "12px 14px",
                                background: "#fff",
                                position: "relative",
                                top: "-4px",
                            }}
                        >
                            <h3
                                style={{
                                    margin: "0 0 10px",
                                    fontSize: "15px",
                                    fontWeight: 600,
                                }}
                            >
                                refund-context
                            </h3>

                            <ContextRow label="paymentId" value={refundContext?.paymentId} />
                            <ContextRow label="status" value={refundContext?.status} isStatus />
                            <ContextRow
                                label="capturedAmount"
                                value={formatAmount(refundContext?.capturedAmount)}
                            />
                            <ContextRow
                                label="refundableAmount"
                                value={formatAmount(refundContext?.refundableAmount)}
                            />
                            <ContextRow label="currency" value={refundContext?.currency} />
                            <ContextRow label="merchantId" value={refundContext?.merchantId} />
                            <ContextRow label="capturedAt" value={formatDateTime(refundContext?.capturedAt)}
                            />
                        </div>
                    </div>
                </div>
            </SectionCard>

            {errorMessage ? <div className="state-error">{errorMessage}</div> : null}

            {successMessage ? (
                <div className="state-success">{successMessage}</div>
            ) : null}

            <SectionCard title="조회 필터">
                <div
                    style={{
                        display: "flex",
                        gap: "10px",
                        alignItems: "flex-end",
                        flexWrap: "wrap",
                    }}
                >
                    <div style={{ minWidth: "132px" }}>
                        <label className="form-field__label">from</label>
                        <input
                            className="input"
                            type="date"
                            name="from"
                            value={filter.from}
                            onChange={handleFilterChange}
                        />
                    </div>

                    <div style={{ minWidth: "132px" }}>
                        <label className="form-field__label">to</label>
                        <input
                            className="input"
                            type="date"
                            name="to"
                            value={filter.to}
                            onChange={handleFilterChange}
                        />
                    </div>

                    <div style={{ minWidth: "132px" }}>
                        <label className="form-field__label">status</label>
                        <select
                            className="input"
                            name="status"
                            value={filter.status}
                            onChange={handleFilterChange}
                        >
                            <option value="ALL">ALL</option>
                            <option value="REQUESTED">REQUESTED</option>
                            <option value="APPROVED">APPROVED</option>
                            <option value="REJECTED">REJECTED</option>
                        </select>
                    </div>

                    <div style={{ flex: 1, minWidth: "260px" }}>
                        <label className="form-field__label">keyword</label>
                        <input
                            className="input"
                            name="keyword"
                            value={filter.keyword}
                            onChange={handleFilterChange}
                            placeholder="refundId / paymentId"
                        />
                    </div>

                    <div>
                        <ActionButton
                            type="button"
                            variant="secondary"
                            onClick={handleFilterReset}
                        >
                            초기화
                        </ActionButton>
                    </div>
                </div>
            </SectionCard>

            <SectionCard title="내 환불 현황">
                {loadingRefunds ? (
                    <div className="state-block">
                        <div className="state-block__title">로딩 중</div>
                        <div className="state-block__description">
                            환불 내역을 불러오고 있습니다.
                        </div>
                    </div>
                ) : filteredRefunds.length === 0 ? (
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
                                <SortHeader
                                    label="refundId"
                                    columnKey="refundId"
                                    sort={sort}
                                    onSortChange={setSort}
                                />
                                <SortHeader
                                    label="paymentId"
                                    columnKey="paymentId"
                                    sort={sort}
                                    onSortChange={setSort}
                                />
                                <SortHeader
                                    label="refundAmount"
                                    columnKey="amount"
                                    sort={sort}
                                    onSortChange={setSort}
                                />
                                <SortHeader
                                    label="status"
                                    columnKey="status"
                                    sort={sort}
                                    onSortChange={setSort}
                                />
                                <SortHeader
                                    label="derived"
                                    columnKey="derived"
                                    sort={sort}
                                    onSortChange={setSort}
                                />
                                <SortHeader
                                    label="requestedAt"
                                    columnKey="requestedAt"
                                    sort={sort}
                                    onSortChange={setSort}
                                />
                                <SortHeader
                                    label="decidedAt"
                                    columnKey="decidedAt"
                                    sort={sort}
                                    onSortChange={setSort}
                                />
                            </tr>
                            </thead>
                            <tbody>
                            {pagedRefunds.map((row) => (
                                <tr key={row.refundId}>
                                    <td
                                        title={row.refundId}
                                        style={{ padding: "10px 18px", lineHeight: 1.2 }}
                                    >
                                        {formatShortId(row.refundId)}
                                    </td>
                                    <td
                                        title={row.paymentId}
                                        style={{ padding: "10px 18px", lineHeight: 1.2 }}
                                    >
                                        {formatShortId(row.paymentId, 10, 6)}
                                    </td>
                                    <td style={{ padding: "10px 18px", lineHeight: 1.2 }}>
                                        {formatAmount(row.amount)}
                                    </td>
                                    <td style={{ padding: "10px 18px", lineHeight: 1.2 }}>
                                        <StatusBadge status={row.status} />
                                    </td>
                                    <td style={{ padding: "10px 18px", lineHeight: 1.2 }}>
                                        {getDerivedLabel(row)}
                                    </td>
                                    <td style={{ padding: "10px 18px", lineHeight: 1.2 }}>
                                        {formatDateTime(row.requestedAt)}
                                    </td>
                                    <td style={{ padding: "10px 18px", lineHeight: 1.2 }}>
                                        {formatDateTime(row.decidedAt)}
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>

                        <div
                            style={{
                                marginTop: "-10px",
                                paddingTop: "0px",
                                display: "flex",
                                justifyContent: "center",
                                alignItems: "center",
                            }}
                        >
                            <div
                                style={{
                                    padding: "10px 0px",
                                    borderRadius: "12px",
                                }}
                            >
                                <Pagination
                                    page={page}
                                    totalPages={totalPages}
                                    onPageChange={setPage}
                                    disabled={loadingRefunds}
                                />
                            </div>
                        </div>
                    </div>
                )}
            </SectionCard>
        </PageLayout>
    );
}