import { useEffect, useState } from "react";
import { useLocation, useSearchParams } from "react-router-dom";
import { getMe } from "../../api/meApi.js";
import RequireLoginNotice from "../../components/feedback/RequireLoginNotice.jsx";
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
import { formatDateTime } from "../../utils/format.js";

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

function isMerchantRole(me) {
    if (!me) {
        return false;
    }

    if (me.role === "MERCHANT" || me.role === "ROLE_MERCHANT") {
        return true;
    }

    if (
        Array.isArray(me.authorities) &&
        me.authorities.includes("ROLE_MERCHANT")
    ) {
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

function formatShortId(value, head = 8, tail = 4) {
    if (!value) return "-";
    if (value.length <= head + tail + 3) return value;
    return `${value.slice(0, head)}...${value.slice(-tail)}`;
}

function formatAmount(value) {
    if (value === null || value === undefined || value === "") return "-";
    return Number(value).toLocaleString("ko-KR");
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
            direction: columnKey === "requestedAt" ? "desc" : "asc",
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
            <span style={{ color: "#667085", fontWeight: 600 }}>{label}</span>

            <span
                style={{
                    textAlign: "right",
                    whiteSpace: "nowrap",
                    overflow: "hidden",
                    textOverflow: "ellipsis",
                }}
                title={String(displayValue)}
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
    const [draftFilter, setDraftFilter] = useState(INITIAL_FILTER);
    const [appliedFilter, setAppliedFilter] = useState(INITIAL_FILTER);
    const [sort, setSort] = useState(INITIAL_SORT);

    const [refundContext, setRefundContext] = useState(null);
    const [refunds, setRefunds] = useState([]);

    const [loading, setLoading] = useState(true);
    const [loadingContext, setLoadingContext] = useState(false);
    const [loadingRefunds, setLoadingRefunds] = useState(false);
    const [submitting, setSubmitting] = useState(false);

    const [errorInfo, setErrorInfo] = useState(null);
    const [errorMessage, setErrorMessage] = useState("");
    const [successMessage, setSuccessMessage] = useState("");

    const [selectedRefundId, setSelectedRefundId] = useState("");

    const [pageInfo, setPageInfo] = useState({
        page: 0,
        size: 7,
        totalElements: 0,
        totalPages: 0,
        hasNext: false,
        hasPrevious: false,
    });

    const [page, setPage] = useState(0);
    const pageSize = 7;

    const [searchParams] = useSearchParams();
    const location = useLocation();

    const initialPaymentId =
        searchParams.get("paymentId")?.trim() ||
        location.state?.paymentId?.trim?.() ||
        "";

    useEffect(() => {
        async function loadPage() {
            try {
                setLoading(true);
                setErrorInfo(null);
                setErrorMessage("");
                setSuccessMessage("");

                const meData = await getMe();

                if (!isMerchantRole(meData)) {
                    setRefunds([]);
                    setRefundContext(null);
                    setErrorInfo({
                        status: 403,
                        message: "Merchant 권한이 필요한 페이지입니다.",
                    });
                    return;
                }

                if (initialPaymentId) {
                    setForm({
                        paymentId: initialPaymentId,
                        amount: "",
                        reasonText: "",
                    });
                    await loadRefundContextByPaymentId(initialPaymentId);
                } else {
                    setForm(INITIAL_FORM);
                    setRefundContext(null);
                }
            } catch (error) {
                setRefunds([]);
                setRefundContext(null);
                setErrorInfo(
                    buildErrorInfo(error, "환불 요청 · 현황 화면을 불러오지 못했습니다.")
                );
            } finally {
                setLoading(false);
            }
        }

        loadPage();
    }, [initialPaymentId]);

    useEffect(() => {
        if (loading || errorInfo) {
            return;
        }

        loadMyRefunds(page);
    }, [loading, errorInfo, page, appliedFilter, sort]);

    async function loadMyRefunds(targetPage = 0) {
        setLoadingRefunds(true);

        try {
            const response = await getMyRefunds({
                status: appliedFilter.status,
                from: appliedFilter.from,
                to: appliedFilter.to,
                keyword: appliedFilter.keyword,
                sortKey: sort.key,
                sortDirection: sort.direction,
                page: targetPage,
                size: pageSize,
            });

            const items = Array.isArray(response?.items) ? response.items : [];
            setRefunds(items);

            setPageInfo({
                page: response?.page ?? targetPage,
                size: response?.size ?? pageSize,
                totalElements: response?.totalElements ?? items.length,
                totalPages: response?.totalPages ?? 0,
                hasNext: response?.hasNext ?? false,
                hasPrevious: response?.hasPrevious ?? false,
            });

            if (selectedRefundId) {
                const exists = items.some((row) => row.refundId === selectedRefundId);
                if (!exists) {
                    setSelectedRefundId("");
                }
            }
        } catch (error) {
            setRefunds([]);
            setPageInfo({
                page: targetPage,
                size: pageSize,
                totalElements: 0,
                totalPages: 0,
                hasNext: false,
                hasPrevious: false,
            });
            setErrorMessage(
                error?.body?.message || "내 환불 내역 조회에 실패했습니다."
            );
        } finally {
            setLoadingRefunds(false);
        }
    }

    async function loadRefundContextByPaymentId(paymentId) {
        if (!paymentId) {
            setRefundContext(null);
            return;
        }

        setLoadingContext(true);

        try {
            const response = await getRefundContext(paymentId);
            setRefundContext(response);
        } catch (error) {
            setRefundContext(null);
            setErrorMessage(
                error?.body?.message ||
                error?.body?.reason ||
                "refund-context 조회에 실패했습니다."
            );
        } finally {
            setLoadingContext(false);
        }
    }

    function handleChange(event) {
        const { name, value } = event.target;
        setForm((prev) => ({
            ...prev,
            [name]: value,
        }));
    }

    function handleDraftFilterChange(event) {
        const { name, value } = event.target;
        setDraftFilter((prev) => ({
            ...prev,
            [name]: value,
        }));
    }

    function handleFilterReset() {
        setDraftFilter(INITIAL_FILTER);
        setAppliedFilter(INITIAL_FILTER);
        setPage(0);
    }

    function handleFilterSubmit(event) {
        event.preventDefault();
        setAppliedFilter({
            ...draftFilter,
            keyword: String(draftFilter.keyword || "").trim(),
        });
        setPage(0);
    }

    function handleSortChange(nextSort) {
        setSort(nextSort);
        setPage(0);
    }

    async function handleLoadContext() {
        const paymentId = form.paymentId.trim();

        if (!paymentId) {
            setRefundContext(null);
            setErrorMessage("paymentId를 입력해주세요.");
            setSuccessMessage("");
            return;
        }

        setErrorMessage("");
        setSuccessMessage("");
        await loadRefundContextByPaymentId(paymentId);
    }

    async function handleSelectRefund(row) {
        setSelectedRefundId(row.refundId);
        setErrorMessage("");
        setSuccessMessage("");

        setForm((prev) => ({
            ...prev,
            paymentId: row.paymentId || "",
            amount: row.amount != null ? String(row.amount) : prev.amount,
            reasonText: row.reasonText || prev.reasonText,
        }));

        await loadRefundContextByPaymentId(row.paymentId || "");
    }

    function handleClearSelectedRefund() {
        setSelectedRefundId("");
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

        if (paymentId !== refundContext.paymentId) {
            setErrorMessage(
                "paymentId를 변경한 경우 refund-context를 다시 조회해 주세요."
            );
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

            setSuccessMessage(
                "현재 상태는 REQUESTED입니다. 관리자 승인 전까지 환불 현황에서 확인할 수 있습니다."
            );
            setSelectedRefundId("");
            setForm((prev) => ({
                ...prev,
                amount: "",
                reasonText: "",
            }));

            await loadRefundContextByPaymentId(paymentId);
            await loadMyRefunds(0);
            setPage(0);
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

    if (loading) {
        return (
            <PageLayout
                title="환불 요청 · 현황"
                description="상단에서 환불 요청을 등록하고, 하단에서 내 환불 현황을 조회합니다."
            >
                <div className="guard-notice">
                    <div className="guard-notice__title">로딩 중</div>
                    <div className="guard-notice__description">
                        환불 요청 · 현황 화면을 불러오는 중입니다.
                    </div>
                </div>
            </PageLayout>
        );
    }

    if (errorInfo?.status === 401) {
        return (
            <PageLayout
                title="환불 요청 · 현황"
                description="상단에서 환불 요청을 등록하고, 하단에서 내 환불 현황을 조회합니다."
            >
                <RequireLoginNotice />
            </PageLayout>
        );
    }

    if (errorInfo?.status === 403) {
        return (
            <PageLayout
                title="환불 요청 · 현황"
                description="상단에서 환불 요청을 등록하고, 하단에서 내 환불 현황을 조회합니다."
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
                title="환불 요청 · 현황"
                description="상단에서 환불 요청을 등록하고, 하단에서 내 환불 현황을 조회합니다."
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

                        <div
                            className="button-row"
                            style={{
                                marginTop: "10px",
                                display: "flex",
                                gap: "8px",
                                flexWrap: "wrap",
                            }}
                        >
                            <ActionButton
                                type="button"
                                onClick={handleSubmit}
                                disabled={submitting}
                            >
                                {submitting ? "요청 중..." : "환불 요청"}
                            </ActionButton>

                            <ActionButton
                                type="button"
                                variant="secondary"
                                onClick={handleClearSelectedRefund}
                                disabled={!selectedRefundId}
                            >
                                선택 해제
                            </ActionButton>
                        </div>

                        {selectedRefundId ? (
                            <div
                                style={{
                                    marginTop: "10px",
                                    fontSize: "12px",
                                    color: "#667085",
                                }}
                            >
                                선택된 환불: {selectedRefundId}
                            </div>
                        ) : null}
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

                            <ContextRow
                                label="paymentId"
                                value={refundContext?.paymentId}
                            />
                            <ContextRow
                                label="status"
                                value={refundContext?.status}
                                isStatus
                            />
                            <ContextRow
                                label="capturedAmount"
                                value={formatAmount(refundContext?.capturedAmount)}
                            />
                            <ContextRow
                                label="refundableAmount"
                                value={formatAmount(refundContext?.refundableAmount)}
                            />
                            <ContextRow
                                label="currency"
                                value={refundContext?.currency}
                            />
                            <ContextRow
                                label="merchantId"
                                value={refundContext?.merchantId}
                            />
                            <ContextRow
                                label="capturedAt"
                                value={formatDateTime(refundContext?.capturedAt)}
                            />
                        </div>
                    </div>
                </div>
            </SectionCard>

            {errorMessage ? <div className="state-error">{errorMessage}</div> : null}

            {successMessage ? (
                <div className="state-success">
                    <strong>환불 요청 등록 완료</strong>
                    <div>{successMessage}</div>
                </div>
            ) : null}

            <SectionCard title="조회 필터">
                <form onSubmit={handleFilterSubmit}>
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
                                value={draftFilter.from}
                                onChange={handleDraftFilterChange}
                            />
                        </div>

                        <div style={{ minWidth: "132px" }}>
                            <label className="form-field__label">to</label>
                            <input
                                className="input"
                                type="date"
                                name="to"
                                value={draftFilter.to}
                                onChange={handleDraftFilterChange}
                            />
                        </div>

                        <div style={{ minWidth: "132px" }}>
                            <label className="form-field__label">status</label>
                            <select
                                className="input"
                                name="status"
                                value={draftFilter.status}
                                onChange={handleDraftFilterChange}
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
                                value={draftFilter.keyword}
                                onChange={handleDraftFilterChange}
                                placeholder="refundId / paymentId"
                            />
                        </div>

                        <div style={{ display: "flex", gap: "8px" }}>
                            <ActionButton type="submit">
                                조회
                            </ActionButton>
                            <ActionButton
                                type="button"
                                variant="secondary"
                                onClick={handleFilterReset}
                            >
                                초기화
                            </ActionButton>
                        </div>
                    </div>
                </form>
            </SectionCard>

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
                                <th style={{ width: "52px", padding: "12px 12px" }}>
                                    선택
                                </th>
                                <SortHeader
                                    label="refundId"
                                    columnKey="refundId"
                                    sort={sort}
                                    onSortChange={handleSortChange}
                                />
                                <SortHeader
                                    label="paymentId"
                                    columnKey="paymentId"
                                    sort={sort}
                                    onSortChange={handleSortChange}
                                />
                                <SortHeader
                                    label="refundAmount"
                                    columnKey="amount"
                                    sort={sort}
                                    onSortChange={handleSortChange}
                                />
                                <SortHeader
                                    label="status"
                                    columnKey="status"
                                    sort={sort}
                                    onSortChange={handleSortChange}
                                />
                                <SortHeader
                                    label="requestedAt"
                                    columnKey="requestedAt"
                                    sort={sort}
                                    onSortChange={handleSortChange}
                                />
                                <SortHeader
                                    label="decidedAt"
                                    columnKey="decidedAt"
                                    sort={sort}
                                    onSortChange={handleSortChange}
                                />
                            </tr>
                            </thead>
                            <tbody>
                            {refunds.map((row) => {
                                const isSelected = selectedRefundId === row.refundId;

                                return (
                                    <tr
                                        key={row.refundId}
                                        onClick={() => handleSelectRefund(row)}
                                        style={{
                                            background: isSelected
                                                ? "#eff6ff"
                                                : "#ffffff",
                                            cursor: "pointer",
                                        }}
                                    >
                                        <td
                                            style={{
                                                padding: "10px 12px",
                                                textAlign: "center",
                                            }}
                                        >
                                            <input
                                                type="radio"
                                                name="selectedRefund"
                                                checked={isSelected}
                                                onChange={() => handleSelectRefund(row)}
                                                onClick={(event) =>
                                                    event.stopPropagation()
                                                }
                                            />
                                        </td>
                                        <td
                                            title={row.refundId}
                                            style={{
                                                padding: "10px 18px",
                                                lineHeight: 1.2,
                                            }}
                                        >
                                            {formatShortId(row.refundId)}
                                        </td>
                                        <td
                                            title={row.paymentId}
                                            style={{
                                                padding: "10px 18px",
                                                lineHeight: 1.2,
                                            }}
                                        >
                                            {formatShortId(row.paymentId, 10, 6)}
                                        </td>
                                        <td
                                            style={{
                                                padding: "10px 18px",
                                                lineHeight: 1.2,
                                            }}
                                        >
                                            {formatAmount(row.amount)}
                                        </td>
                                        <td
                                            style={{
                                                padding: "10px 18px",
                                                lineHeight: 1.2,
                                            }}
                                        >
                                            <StatusBadge status={row.status} />
                                        </td>
                                        <td
                                            style={{
                                                padding: "10px 18px",
                                                lineHeight: 1.2,
                                            }}
                                        >
                                            {formatDateTime(row.requestedAt)}
                                        </td>
                                        <td
                                            style={{
                                                padding: "10px 18px",
                                                lineHeight: 1.2,
                                            }}
                                        >
                                            {formatDateTime(row.decidedAt)}
                                        </td>
                                    </tr>
                                );
                            })}
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
                                    page={pageInfo.page}
                                    totalPages={pageInfo.totalPages}
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