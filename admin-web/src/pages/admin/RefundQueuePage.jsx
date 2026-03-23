import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import PageLayout from "../../components/layout/PageLayout.jsx";
import SectionCard from "../../components/layout/SectionCard.jsx";
import ActionButton from "../../components/layout/ActionButton.jsx";
import StatusBadge from "../../components/display/StatusBadge.jsx";
import CopyableId from "../../components/display/CopyableId.jsx";
import AmountText from "../../components/display/AmountText.jsx";
import Pagination from "../../components/table/Pagination.jsx";
import LoadingBlock from "../../components/feedback/LoadingBlock.jsx";
import EmptyState from "../../components/feedback/EmptyState.jsx";
import ErrorState from "../../components/feedback/ErrorState.jsx";
import usePagination from "../../hooks/usePagination.js";
import {
    getAdminRefunds,
    approveRefund,
    rejectRefund,
} from "../../api/refundApi.js";
import { formatDateTime } from "../../util/format.js";

const INITIAL_FILTERS = {
    status: "REQUESTED",
    from: "",
    to: "",
};

const INITIAL_SORT = {
    key: "requestedAt",
    direction: "desc",
};

function normalizeText(value) {
    return String(value ?? "").trim().toLowerCase();
}

function getRequestedAtTime(row) {
    if (!row?.requestedAt) return 0;
    const time = new Date(row.requestedAt).getTime();
    return Number.isNaN(time) ? 0 : time;
}

function getAmountValue(row, primaryKey, fallbackKeys = []) {
    const keys = [primaryKey, ...fallbackKeys];

    for (const key of keys) {
        const value = row?.[key];
        if (value !== undefined && value !== null && value !== "") {
            return value;
        }
    }

    return null;
}

function renderText(value) {
    if (value === undefined || value === null || value === "") {
        return "-";
    }
    return value;
}

function renderDate(value) {
    if (!value) return "-";
    return formatDateTime(value);
}

function getSortableValue(row, sortKey) {
    switch (sortKey) {
        case "refundId":
            return String(row?.refundId ?? "").toLowerCase();
        case "paymentId":
            return String(row?.paymentId ?? "").toLowerCase();
        case "amount":
            return Number(getAmountValue(row, "amount", ["refundAmount"]) ?? -1);
        case "capturedAmount":
            return Number(getAmountValue(row, "capturedAmount") ?? -1);
        case "refundableAmount":
            return Number(getAmountValue(row, "refundableAmount") ?? -1);
        case "requestedAt":
            return getRequestedAtTime(row);
        default:
            return "";
    }
}

export default function RefundQueuePage() {
    const navigate = useNavigate();
    const { page, size, setPage, resetPage } = usePagination(0, 7);

    const [filters] = useState(INITIAL_FILTERS);
    const [draftKeyword, setDraftKeyword] = useState("");
    const [appliedKeyword, setAppliedKeyword] = useState("");
    const [sortState, setSortState] = useState(INITIAL_SORT);

    const [refunds, setRefunds] = useState([]);
    const [pageInfo, setPageInfo] = useState({
        page: 0,
        size: 7,
        totalElements: 0,
        totalPages: 0,
        hasNext: false,
        hasPrevious: false,
    });

    const [selectedRefundId, setSelectedRefundId] = useState("");
    const [comment, setComment] = useState("");
    const [lastActionResult, setLastActionResult] = useState(null);

    const [loading, setLoading] = useState(false);
    const [acting, setActing] = useState(false);
    const [errorMessage, setErrorMessage] = useState("");

    useEffect(() => {
        loadRefunds(page);
    }, [page, size]);

    async function loadRefunds(targetPage = 0) {
        setLoading(true);
        setErrorMessage("");

        try {
            const response = await getAdminRefunds({
                status: filters.status,
                from: filters.from,
                to: filters.to,
                page: targetPage,
                size,
            });

            const items = Array.isArray(response?.items) ? response.items : [];
            setRefunds(items);

            setPageInfo({
                page: response?.page ?? targetPage,
                size: response?.size ?? size,
                totalElements: response?.totalElements ?? items.length,
                totalPages: response?.totalPages ?? 0,
                hasNext: response?.hasNext ?? false,
                hasPrevious: response?.hasPrevious ?? false,
            });

            if (selectedRefundId) {
                const exists = items.some((row) => row.refundId === selectedRefundId);
                if (!exists) {
                    setSelectedRefundId("");
                    setComment("");
                }
            }
        } catch (error) {
            setErrorMessage(error?.body?.message || "환불 큐 조회에 실패했습니다.");
            setRefunds([]);
            setPageInfo({
                page: targetPage,
                size,
                totalElements: 0,
                totalPages: 0,
                hasNext: false,
                hasPrevious: false,
            });
        } finally {
            setLoading(false);
        }
    }

    const filteredRefunds = useMemo(() => {
        const keyword = normalizeText(appliedKeyword);
        let rows = [...refunds];

        if (keyword) {
            rows = rows.filter((row) => {
                const refundId = normalizeText(row?.refundId);
                const paymentId = normalizeText(row?.paymentId);
                return refundId.includes(keyword) || paymentId.includes(keyword);
            });
        }

        rows.sort((a, b) => {
            const aValue = getSortableValue(a, sortState.key);
            const bValue = getSortableValue(b, sortState.key);

            if (aValue < bValue) {
                return sortState.direction === "asc" ? -1 : 1;
            }
            if (aValue > bValue) {
                return sortState.direction === "asc" ? 1 : -1;
            }
            return 0;
        });

        return rows;
    }, [refunds, appliedKeyword, sortState]);

    const selectedRow =
        filteredRefunds.find((row) => row.refundId === selectedRefundId) ||
        refunds.find((row) => row.refundId === selectedRefundId) ||
        null;

    const selectedStatus = String(selectedRow?.status || "").toUpperCase();
    const canAct =
        !!selectedRow &&
        selectedStatus === "REQUESTED" &&
        !acting &&
        String(comment).trim().length > 0;
    const canTraceToA1 = Boolean(lastActionResult?.requestId);

    function handleSearch(event) {
        event.preventDefault();
        setErrorMessage("");
        setLastActionResult(null);
        setAppliedKeyword(draftKeyword);

        if (page !== 0) {
            resetPage();
            return;
        }

        loadRefunds(0);
    }

    function handleSelectRefund(row) {
        setSelectedRefundId(row.refundId);
        setComment("");
        setErrorMessage("");
        setLastActionResult(null);
    }

    function handleClearSelection() {
        setSelectedRefundId("");
        setComment("");
        setErrorMessage("");
        setLastActionResult(null);
    }

    function handleSort(sortKey) {
        setSortState((prev) => {
            if (prev.key === sortKey) {
                return {
                    key: sortKey,
                    direction: prev.direction === "asc" ? "desc" : "asc",
                };
            }

            return {
                key: sortKey,
                direction: sortKey === "requestedAt" ? "desc" : "asc",
            };
        });
    }

    function renderSortArrow(sortKey) {
        if (sortState.key !== sortKey) return "↕";
        return sortState.direction === "asc" ? "↑" : "↓";
    }

    async function handleDecision(actionType) {
        if (!selectedRow) {
            setErrorMessage("환불 1건을 먼저 선택해야 합니다.");
            return;
        }

        if (selectedStatus !== "REQUESTED") {
            setErrorMessage("REQUESTED 상태에서만 승인/거절할 수 있습니다.");
            return;
        }

        const trimmedComment = String(comment || "").trim();

        if (!trimmedComment) {
            setErrorMessage("approve/reject에는 comment가 필수입니다.");
            return;
        }

        setActing(true);
        setErrorMessage("");
        setLastActionResult(null);

        try {
            const result =
                actionType === "approve"
                    ? await approveRefund(selectedRow.refundId, trimmedComment)
                    : await rejectRefund(selectedRow.refundId, trimmedComment);

            setLastActionResult({
                refundId: selectedRow.refundId,
                actionType,
                status: result?.status,
                decidedAt: result?.decidedAt,
                requestId: result?.requestId,
                comment: trimmedComment,
            });

            setComment("");
            await loadRefunds(page);
            setSelectedRefundId("");
        } catch (error) {
            if (error?.body?.reason) {
                setErrorMessage(`환불 처리 실패: ${error.body.reason}`);
            } else {
                setErrorMessage(error?.body?.message || "환불 처리에 실패했습니다.");
            }
        } finally {
            setActing(false);
        }
    }

    function moveToTrace() {
        const requestId = lastActionResult?.requestId;
        if (!requestId) return;
        navigate(`/admin/audit?requestId=${encodeURIComponent(requestId)}`);
    }

    return (
        <PageLayout
            title="환불 큐"
            description="A6 환불 큐 단일 선택 승인/거절 화면입니다."
        >
            <style>{`
                .refund-queue-one-page {
                    display: flex;
                    flex-direction: column;
                    gap: 18px;
                }

                .refund-queue-layout {
                    display: flex;
                    flex-direction: column;
                    gap: 18px;
                }

                .refund-queue-panel {
                    min-width: 0;
                }

                .refund-queue-toolbar {
                    display: grid;
                    grid-template-columns: 1fr;
                    gap: 12px;
                    align-items: end;
                    margin-bottom: 12px;
                }

                .refund-queue-toolbar .form-field__label {
                    display: block;
                    margin-bottom: 6px;
                    font-size: 13px;
                    font-weight: 700;
                    color: #475467;
                }

                .refund-queue-toolbar .input {
                    width: 80%;
                }

                .refund-queue-toolbar-actions {
                    display: flex;
                    gap: 8px;
                    margin-bottom: 14px;
                }

                .refund-queue-table-wrap {
                    width: 100%;
                    overflow-x: visible;
                    overflow-y: hidden;
                    border: 1px solid var(--color-border);
                    border-radius: var(--radius-lg);
                    background: var(--color-surface);
                }

                .refund-queue-table {
                    width: 100%;
                    border-collapse: separate;
                    border-spacing: 0;
                    table-layout: fixed;
                }

                .refund-queue-table th,
                .refund-queue-table td {
                    padding: 11px 12px;
                    border-bottom: 1px solid var(--color-border);
                    vertical-align: middle;
                }

                .refund-queue-table th {
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                }

                .refund-queue-table td {
                    overflow: hidden;
                }

                .refund-queue-table thead th {
                    background: #f8fafc;
                    color: #475467;
                    font-size: 13px;
                    font-weight: 700;
                    text-align: left;
                }

                .refund-queue-sort-button {
                    display: inline-flex;
                    align-items: center;
                    gap: 6px;
                    padding: 0;
                    border: 0;
                    background: transparent;
                    color: inherit;
                    font: inherit;
                    font-weight: 700;
                    cursor: pointer;
                }

                .refund-queue-sort-button:hover {
                    color: #101828;
                }

                .refund-queue-sort-arrow {
                    font-size: 12px;
                    color: #667085;
                    line-height: 1;
                }

                .refund-queue-table tbody tr {
                    cursor: pointer;
                    transition: background-color 0.15s ease;
                }

                .refund-queue-table tbody tr:hover {
                    background: #f8fafc;
                }

                .refund-queue-table tbody tr.is-selected {
                    background: #eff6ff;
                }

                .refund-queue-radio-col {
                    width: 72px;
                }

                .refund-queue-id-col {
                    width: 24%;
                }

                .refund-queue-payment-col {
                    width: 24%;
                }

                .refund-queue-amount-col {
                    width: 13%;
                }

                .refund-queue-captured-col {
                    width: 11%;
                }

                .refund-queue-refundable-col {
                    width: 11%;
                }

                .refund-queue-datetime-col {
                    width: 17%;
                    color: #667085;
                    font-variant-numeric: tabular-nums;
                }

                .refund-queue-radio {
                    display: flex;
                    align-items: center;
                    justify-content: center;
                }

                .refund-queue-radio input {
                    width: 16px;
                    height: 16px;
                    cursor: pointer;
                }

                .refund-queue-note {
                    margin-top: 8px;
                    font-size: 12px;
                    color: #667085;
                }

                .refund-queue-pagination {
                    display: flex;
                    justify-content: center;
                    margin-top: 14px;
                }

                .refund-queue-detail-card {
                    border: 1px solid var(--color-border);
                    border-radius: var(--radius-lg);
                    background: var(--color-surface);
                    padding: 14px;
                }

                .refund-queue-guide {
                    padding: 9px 12px;
                    border: 1px solid #d0d5dd;
                    border-radius: 10px;
                    background: #f8fafc;
                    color: #667085;
                    font-size: 12px;
                    line-height: 1.4;
                    margin-bottom: 12px;
                }

                .refund-queue-result {
                    margin-bottom: 12px;
                    padding: 10px 12px;
                    border: 1px solid #d0d5dd;
                    border-radius: 10px;
                    background: #f8fafc;
                }

                .refund-queue-summary-grid {
                    display: grid;
                    grid-template-columns: repeat(3, minmax(0, 1fr));
                    gap: 10px;
                    margin-bottom: 10px;
                }

                .refund-queue-summary-card {
                    border: 1px solid var(--color-border);
                    border-radius: 10px;
                    background: #fcfcfd;
                    padding: 10px 12px;
                    min-width: 0;
                }

                .refund-queue-summary-label {
                    font-size: 11px;
                    color: #667085;
                    margin-bottom: 4px;
                }

                .refund-queue-summary-value {
                    font-size: 14px;
                    font-weight: 700;
                    color: #101828;
                    min-width: 0;
                    word-break: break-word;
                    overflow-wrap: anywhere;
                }

                .refund-queue-summary-value .copyable-id {
                    display: inline-flex;
                    align-items: center;
                    gap: 6px;
                    max-width: 100%;
                    flex-wrap: wrap;
                }

                .refund-queue-summary-value .copyable-id__text {
                    min-width: 0;
                    max-width: 100%;
                    white-space: normal;
                    overflow: visible;
                    text-overflow: unset;
                    word-break: break-word;
                    overflow-wrap: anywhere;
                }

                .refund-queue-inline-grid {
                    display: grid;
                    grid-template-columns: repeat(3, minmax(0, 1fr));
                    gap: 8px;
                    margin-bottom: 10px;
                }
                
                .refund-queue-inline-item {
                    display: flex;
                    align-items: center;
                    gap: 8px;
                    min-width: 0;
                    border: 1px solid var(--color-border);
                    border-radius: 10px;
                    background: #fcfcfd;
                    padding: 9px 12px;
                }
                
                .refund-queue-inline-item--wide {
                    grid-column: 1 / -1;
                }
                
                .refund-queue-inline-label {
                    flex: 0 0 auto;
                    font-size: 12px;
                    color: #667085;
                    font-weight: 600;
                }
                
                .refund-queue-inline-value {
                    min-width: 0;
                    font-size: 13px;
                    color: #101828;
                    font-weight: 600;
                    word-break: break-word;
                    overflow-wrap: anywhere;
                }
                
                .refund-queue-inline-value .amount-text {
                    font-size: 13px;
                }
                
                .refund-queue-inline-value .status-badge {
                    vertical-align: middle;
                }

                .refund-queue-comment-block {
                    margin-top: 0;
                }

                .refund-queue-comment-label {
                    display: block;
                    margin-bottom: 8px;
                    font-size: 13px;
                    font-weight: 700;
                    color: #344054;
                }

                .refund-queue-comment-label .required {
                    color: #d92d20;
                }

                .refund-queue-comment-textarea {
                    width: 100%;
                    min-height: 72px;
                    resize: vertical;
                    box-sizing: border-box;
                }

                .refund-queue-detail-actions {
                    display: flex;
                    gap: 8px;
                    align-items: center;
                    margin-top: 12px;
                    flex-wrap: wrap;
                }

                .refund-queue-detail-actions > * {
                    flex: 1 1 0;
                    min-width: 0;
                }

                .refund-queue-meta-note {
                    margin-top: 10px;
                    font-size: 12px;
                    color: #667085;
                    white-space: pre-wrap;
                    line-height: 1.5;
                }

                .refund-queue-table-cell-copy .copyable-id {
                    display: inline-flex;
                    align-items: center;
                    gap: 6px;
                    max-width: 100%;
                }

                .refund-queue-table-cell-copy .copyable-id__text {
                    display: inline-block;
                    max-width: 100%;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                }

                @media (max-width: 1200px) {
                    .refund-queue-summary-grid,
                    .refund-queue-inline-grid {
                        grid-template-columns: repeat(2, minmax(0, 1fr));
                    }
                
                    .refund-queue-inline-item--wide {
                        grid-column: 1 / -1;
                    }
                }
                
                @media (max-width: 768px) {
                    .refund-queue-toolbar-actions,
                    .refund-queue-detail-actions {
                        flex-direction: column;
                    }
                
                    .refund-queue-detail-actions > * {
                        width: 100%;
                    }
                
                    .refund-queue-summary-grid,
                    .refund-queue-inline-grid {
                        grid-template-columns: 1fr;
                    }
                
                    .refund-queue-inline-item,
                    .refund-queue-inline-item--wide {
                        grid-column: auto;
                    }
                }
            `}</style>

            <div className="refund-queue-one-page">
                {errorMessage ? <ErrorState message={errorMessage} /> : null}

                <div className="refund-queue-layout">
                    <div className="refund-queue-panel">
                        <SectionCard title="선택 상세/결정">
                            <div className="refund-queue-guide">
                                선택된 1건을 승인/거절합니다. 이미 결정된 건 재호출은
                                no-op 200(+status, decidedAt) 규격입니다.
                            </div>
                            {lastActionResult ? (
                                <div className="refund-queue-result">
                                    <div style={{ fontWeight: 700, marginBottom: 6 }}>
                                        처리 결과
                                    </div>
                                    <div style={{ fontSize: 13, lineHeight: 1.5 }}>
                                        <div>refundId: {lastActionResult.refundId}</div>
                                        <div>status: {lastActionResult.status || "-"}</div>
                                        <div>decidedAt: {renderDate(lastActionResult.decidedAt)}</div>
                                    </div>

                                    <div className="refund-queue-detail-actions">
                                        <ActionButton
                                            type="button"
                                            variant="secondary"
                                            onClick={moveToTrace}
                                            disabled={!lastActionResult.requestId}
                                        >
                                            Trace로 보기(A1)
                                        </ActionButton>
                                    </div>
                                </div>
                            ) : null}

                            <div className="refund-queue-detail-card">

                                <div className="refund-queue-summary-grid">
                                    <div className="refund-queue-summary-card">
                                        <div className="refund-queue-summary-label">refundId</div>
                                        <div className="refund-queue-summary-value">
                                            {selectedRow ? (
                                                <CopyableId value={selectedRow.refundId} short />
                                            ) : (
                                                "-"
                                            )}
                                        </div>
                                    </div>

                                    <div className="refund-queue-summary-card">
                                        <div className="refund-queue-summary-label">paymentId</div>
                                        <div className="refund-queue-summary-value">
                                            {selectedRow ? (
                                                <CopyableId value={selectedRow.paymentId} short />
                                            ) : (
                                                "-"
                                            )}
                                        </div>
                                    </div>

                                    <div className="refund-queue-summary-card">
                                        <div className="refund-queue-summary-label">merchantId</div>
                                        <div className="refund-queue-summary-value">
                                            {renderText(selectedRow?.merchantId)}
                                        </div>
                                    </div>
                                </div>

                                <div className="refund-queue-inline-grid">
                                    <div className="refund-queue-inline-item">
                                        <span className="refund-queue-inline-label">status</span>
                                        <span className="refund-queue-inline-value">
                                            {selectedRow ? <StatusBadge status={selectedRow.status} /> : "-"}
                                        </span>
                                    </div>

                                    <div className="refund-queue-inline-item">
                                        <span className="refund-queue-inline-label">requestedAt</span>
                                        <span className="refund-queue-inline-value">
                                            {renderDate(selectedRow?.requestedAt)}
                                        </span>
                                    </div>

                                    <div className="refund-queue-inline-item">
                                        <span className="refund-queue-inline-label">decidedAt</span>
                                        <span className="refund-queue-inline-value">
                                            {renderDate(selectedRow?.decidedAt)}
                                        </span>
                                    </div>

                                    <div className="refund-queue-inline-item">
                                        <span className="refund-queue-inline-label">captured</span>
                                        <span className="refund-queue-inline-value">
                                            <AmountText value={getAmountValue(selectedRow, "capturedAmount")} />
                                        </span>
                                    </div>

                                    <div className="refund-queue-inline-item">
                                        <span className="refund-queue-inline-label">refundable</span>
                                        <span className="refund-queue-inline-value">
                                            <AmountText value={getAmountValue(selectedRow, "refundableAmount")} />
                                        </span>
                                    </div>

                                    <div className="refund-queue-inline-item">
                                        <span className="refund-queue-inline-label">refund</span>
                                        <span className="refund-queue-inline-value">
                                            <AmountText value={getAmountValue(selectedRow, "amount", ["refundAmount"])}
                                            />
                                        </span>
                                    </div>

                                    <div className="refund-queue-inline-item refund-queue-inline-item--wide">
                                        <span className="refund-queue-inline-label">요청 메모</span>
                                        <span className="refund-queue-inline-value">
                                            {renderText(selectedRow?.reasonText)}
                                        </span>
                                    </div>
                                </div>

                                <div className="refund-queue-comment-block">
                                    <label className="refund-queue-comment-label">
                                        운영 메모(comment){" "}
                                        <span className="required">(필수)</span>
                                    </label>
                                    <textarea
                                        className="textarea refund-queue-comment-textarea"
                                        value={comment}
                                        onChange={(event) => setComment(event.target.value)}
                                        placeholder="승인/거절 사유를 입력하세요."
                                        disabled={acting || selectedStatus !== "REQUESTED"}
                                    />
                                </div>

                                <div className="refund-queue-detail-actions">
                                    <ActionButton
                                        type="button"
                                        disabled={!canAct}
                                        onClick={() => handleDecision("approve")}
                                    >
                                        {acting ? "처리 중..." : "승인(Approve)"}
                                    </ActionButton>

                                    <ActionButton
                                        type="button"
                                        variant="danger"
                                        disabled={!canAct}
                                        onClick={() => handleDecision("reject")}
                                    >
                                        {acting ? "처리 중..." : "거절(Reject)"}
                                    </ActionButton>
                                </div>

                                <div className="refund-queue-meta-note">
                                    {`* 409 포맷: {"code":"RULE_VIOLATION","reason":"INSUFFICIENT_REFUNDABLE"}`}
                                </div>
                            </div>
                        </SectionCard>
                    </div>

                    <div className="refund-queue-panel">
                        <SectionCard title="대기 큐(REQUESTED)">
                            <form onSubmit={handleSearch}>
                                <div className="refund-queue-toolbar">
                                    <div>
                                        <label className="form-field__label">
                                            키워드(환불ID/결제ID)
                                        </label>
                                        <input
                                            className="input"
                                            type="text"
                                            value={draftKeyword}
                                            onChange={(event) => setDraftKeyword(event.target.value)}
                                            placeholder="예: RFD-20001 또는 PAY-90011"
                                        />
                                    </div>
                                    <div className="refund-queue-toolbar-actions">
                                        <ActionButton type="submit" disabled={loading}>
                                            조회
                                        </ActionButton>
                                        <ActionButton
                                            type="button"
                                            variant="secondary"
                                            onClick={handleClearSelection}
                                            disabled={!selectedRefundId && !comment}
                                        >
                                            선택 해제
                                        </ActionButton>
                                    </div>
                                </div>
                            </form>

                            {loading ? (
                                <LoadingBlock
                                    title="로딩 중"
                                    description="환불 대기 큐를 불러오고 있습니다."
                                />
                            ) : filteredRefunds.length === 0 ? (
                                <EmptyState
                                    title="대기 환불 없음"
                                    description="REQUESTED 상태의 환불이 없습니다."
                                />
                            ) : (
                                <>
                                    <div className="refund-queue-table-wrap">
                                        <table className="refund-queue-table">
                                            <thead>
                                            <tr>
                                                <th className="refund-queue-radio-col">선택</th>
                                                <th className="refund-queue-id-col">
                                                    <button
                                                        type="button"
                                                        className="refund-queue-sort-button"
                                                        onClick={() => handleSort("refundId")}
                                                    >
                                                        refundId
                                                        <span className="refund-queue-sort-arrow">
                                                                {renderSortArrow("refundId")}
                                                            </span>
                                                    </button>
                                                </th>
                                                <th className="refund-queue-payment-col">
                                                    <button
                                                        type="button"
                                                        className="refund-queue-sort-button"
                                                        onClick={() => handleSort("paymentId")}
                                                    >
                                                        paymentId
                                                        <span className="refund-queue-sort-arrow">
                                                                {renderSortArrow("paymentId")}
                                                            </span>
                                                    </button>
                                                </th>
                                                <th className="refund-queue-amount-col">
                                                    <button
                                                        type="button"
                                                        className="refund-queue-sort-button"
                                                        onClick={() => handleSort("amount")}
                                                    >
                                                        amount
                                                        <span className="refund-queue-sort-arrow">
                                                                {renderSortArrow("amount")}
                                                            </span>
                                                    </button>
                                                </th>
                                                <th className="refund-queue-captured-col">
                                                    <button
                                                        type="button"
                                                        className="refund-queue-sort-button"
                                                        onClick={() => handleSort("capturedAmount")}
                                                    >
                                                        captured
                                                        <span className="refund-queue-sort-arrow">
                                                                {renderSortArrow("capturedAmount")}
                                                            </span>
                                                    </button>
                                                </th>
                                                <th className="refund-queue-refundable-col">
                                                    <button
                                                        type="button"
                                                        className="refund-queue-sort-button"
                                                        onClick={() =>
                                                            handleSort("refundableAmount")
                                                        }
                                                    >
                                                        refundable
                                                        <span className="refund-queue-sort-arrow">
                                                                {renderSortArrow("refundableAmount")}
                                                            </span>
                                                    </button>
                                                </th>
                                                <th className="refund-queue-datetime-col">
                                                    <button
                                                        type="button"
                                                        className="refund-queue-sort-button"
                                                        onClick={() => handleSort("requestedAt")}
                                                    >
                                                        requestedAt
                                                        <span className="refund-queue-sort-arrow">
                                                                {renderSortArrow("requestedAt")}
                                                            </span>
                                                    </button>
                                                </th>
                                            </tr>
                                            </thead>
                                            <tbody>
                                            {filteredRefunds.map((row) => {
                                                const isSelected =
                                                    row.refundId === selectedRefundId;

                                                return (
                                                    <tr
                                                        key={row.refundId}
                                                        className={isSelected ? "is-selected" : ""}
                                                        onClick={() => handleSelectRefund(row)}
                                                    >
                                                        <td className="refund-queue-radio-col">
                                                            <div className="refund-queue-radio">
                                                                <input
                                                                    type="radio"
                                                                    name="refundSelection"
                                                                    checked={isSelected}
                                                                    onChange={() =>
                                                                        handleSelectRefund(row)
                                                                    }
                                                                    onClick={(event) =>
                                                                        event.stopPropagation()
                                                                    }
                                                                    aria-label={`${row.refundId} 선택`}
                                                                />
                                                            </div>
                                                        </td>
                                                        <td className="refund-queue-id-col refund-queue-table-cell-copy">
                                                            <CopyableId value={row.refundId} short />
                                                        </td>
                                                        <td className="refund-queue-payment-col refund-queue-table-cell-copy">
                                                            <CopyableId value={row.paymentId} short />
                                                        </td>
                                                        <td className="refund-queue-amount-col">
                                                            <AmountText
                                                                value={getAmountValue(row, "amount", [
                                                                    "refundAmount",
                                                                ])}
                                                            />
                                                        </td>
                                                        <td className="refund-queue-captured-col">
                                                            <AmountText
                                                                value={getAmountValue(
                                                                    row,
                                                                    "capturedAmount"
                                                                )}
                                                            />
                                                        </td>
                                                        <td className="refund-queue-refundable-col">
                                                            <AmountText
                                                                value={getAmountValue(
                                                                    row,
                                                                    "refundableAmount"
                                                                )}
                                                            />
                                                        </td>
                                                        <td className="refund-queue-datetime-col">
                                                            {formatDateTime(row.requestedAt)}
                                                        </td>
                                                    </tr>
                                                );
                                            })}
                                            </tbody>
                                        </table>
                                    </div>

                                    <div className="refund-queue-note">
                                        * 라디오를 직접 클릭해도 되고, 행 아무데나 클릭해도 선택됩니다.
                                    </div>

                                    <div className="refund-queue-pagination">
                                        <Pagination
                                            page={pageInfo.page}
                                            totalPages={pageInfo.totalPages}
                                            onPageChange={setPage}
                                            disabled={loading}
                                        />
                                    </div>
                                </>
                            )}
                        </SectionCard>
                    </div>
                </div>
            </div>
        </PageLayout>
    );
}