import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { getMe } from "../../api/meApi.js";
import RequireLoginNotice from "../../components/feedback/RequireLoginNotice.jsx";
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
    getAdminRefundTraceEntry,
} from "../../api/refundApi.js";
import { formatDateTime } from "../../utils/format.js";

const INITIAL_FILTERS = {
    status: "REQUESTED",
    from: "",
    to: "",
    keyword: "",
};

const INITIAL_SORT = {
    key: "requestedAt",
    direction: "desc",
};

function isAdminRole(me) {
    if (!me) return false;

    if (me.role === "ADMIN" || me.role === "ROLE_ADMIN") {
        return true;
    }

    if (Array.isArray(me.authorities) && me.authorities.includes("ROLE_ADMIN")) {
        return true;
    }

    return false;
}

function buildErrorInfo(error, fallbackMessage) {
    return {
        status: error?.status ?? null,
        message: error?.body?.message || error?.body?.reason || fallbackMessage,
    };
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

export default function RefundQueuePage() {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const { page, size, setPage, resetPage } = usePagination(0, 7);

    const anchorSettlementId = useMemo(() => {
        const value = searchParams.get("settlementId");
        return value && value.trim() ? value.trim() : "";
    }, [searchParams]);

    const [draftFilters, setDraftFilters] = useState(INITIAL_FILTERS);
    const [appliedFilters, setAppliedFilters] = useState(INITIAL_FILTERS);
    const [sortState, setSortState] = useState(INITIAL_SORT);

    const [me, setMe] = useState(null);
    const [meLoading, setMeLoading] = useState(true);
    const [meErrorInfo, setMeErrorInfo] = useState(null);

    const [refunds, setRefunds] = useState([]);
    const [pageInfo, setPageInfo] = useState({
        page: 0,
        size: 7,
        totalElements: 0,
        totalPages: 0,
        hasNext: false,
        hasPrevious: false,
    });

    const [selectedRefund, setSelectedRefund] = useState(null);

    const [comment, setComment] = useState("");
    const [lastActionResult, setLastActionResult] = useState(null);
    const [traceRequestId, setTraceRequestId] = useState("");
    const [traceLoading, setTraceLoading] = useState(false);

    const [loading, setLoading] = useState(false);
    const [acting, setActing] = useState(false);
    const [errorMessage, setErrorMessage] = useState("");

    const selectedRow = selectedRefund;
    const selectedRefundId = selectedRefund?.refundId || "";
    const selectedStatus = String(selectedRow?.status || "").toUpperCase();

    const prevAnchorSettlementIdRef = useRef(anchorSettlementId);

    useEffect(() => {
        let cancelled = false;

        async function loadMe() {
            try {
                setMeLoading(true);
                setMeErrorInfo(null);

                const meData = await getMe();

                if (cancelled) return;

                setMe(meData);

                if (!isAdminRole(meData)) {
                    setMeErrorInfo({
                        status: 403,
                        message: "Admin 권한이 필요한 페이지입니다.",
                    });
                }
            } catch (error) {
                if (cancelled) return;

                setMe(null);
                setMeErrorInfo(buildErrorInfo(error, "권한 정보를 불러오지 못했습니다."));
            } finally {
                if (!cancelled) {
                    setMeLoading(false);
                }
            }
        }

        loadMe();

        return () => {
            cancelled = true;
        };
    }, []);

    useEffect(() => {
        const prevAnchorSettlementId = prevAnchorSettlementIdRef.current;

        if (prevAnchorSettlementId === anchorSettlementId) {
            return;
        }

        prevAnchorSettlementIdRef.current = anchorSettlementId;

        setSelectedRefund(null);
        setComment("");
        setLastActionResult(null);
        setTraceRequestId("");
        setTraceLoading(false);
        setErrorMessage("");

        setDraftFilters((prev) => ({
            ...prev,
            keyword: "",
        }));

        setAppliedFilters((prev) => ({
            ...prev,
            keyword: "",
        }));

        if (page !== 0) {
            resetPage();
        }
    }, [anchorSettlementId, page, resetPage]);

    useEffect(() => {
        if (meLoading || meErrorInfo || !isAdminRole(me)) {
            return;
        }

        let cancelled = false;

        async function loadRefunds(targetPage = 0) {
            try {
                setLoading(true);
                setErrorMessage("");

                const response = await getAdminRefunds({
                    status: appliedFilters.status,
                    from: appliedFilters.from,
                    to: appliedFilters.to,
                    settlementId: anchorSettlementId || undefined,
                    keyword: appliedFilters.keyword,
                    sortKey: sortState.key,
                    sortDirection: sortState.direction,
                    page: targetPage,
                    size,
                });

                if (cancelled) return;

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

                setSelectedRefund((prev) => {
                    if (prev?.refundId) {
                        const matched = items.find((row) => row.refundId === prev.refundId);
                        return matched || null;
                    }

                    if (anchorSettlementId && items.length === 1) {
                        return items[0];
                    }

                    return null;
                });
            } catch (error) {
                if (cancelled) return;

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
                setSelectedRefund(null);
                setComment("");
                setTraceRequestId("");
                setTraceLoading(false);
            } finally {
                if (!cancelled) {
                    setLoading(false);
                }
            }
        }

        loadRefunds(page);

        return () => {
            cancelled = true;
        };
    }, [
        meLoading,
        meErrorInfo,
        me,
        page,
        size,
        appliedFilters,
        sortState,
        anchorSettlementId,
    ]);

    useEffect(() => {
        if (!selectedRow?.refundId) {
            setTraceRequestId("");
            setTraceLoading(false);
            return;
        }

        let cancelled = false;

        async function loadTraceEntry() {
            try {
                setTraceLoading(true);

                const response = await getAdminRefundTraceEntry(selectedRow.refundId);

                if (cancelled) return;

                const requestId =
                    response?.traceRequestId ||
                    response?.requestId ||
                    response?.resolvedRequestId ||
                    "";

                setTraceRequestId(requestId);
            } catch (error) {
                if (cancelled) return;
                setTraceRequestId("");
            } finally {
                if (!cancelled) {
                    setTraceLoading(false);
                }
            }
        }

        loadTraceEntry();

        return () => {
            cancelled = true;
        };
    }, [selectedRow?.refundId]);

    const canAct =
        !!selectedRow &&
        selectedStatus === "REQUESTED" &&
        !acting &&
        String(comment).trim().length > 0;

    function handleDraftFilterChange(event) {
        const { name, value } = event.target;
        setDraftFilters((prev) => ({
            ...prev,
            [name]: value,
        }));
    }

    function handleSearch(event) {
        event.preventDefault();

        const nextFilters = {
            ...draftFilters,
            keyword: String(draftFilters.keyword || "").trim(),
        };

        setErrorMessage("");
        setLastActionResult(null);
        setAppliedFilters(nextFilters);

        if (page !== 0) {
            resetPage();
        }
    }

    function handleSelectRefund(row) {
        setSelectedRefund(row);
        setComment("");
        setErrorMessage("");
        setLastActionResult(null);
        setTraceRequestId("");
    }

    function handleClearSelection() {
        setSelectedRefund(null);
        setComment("");
        setErrorMessage("");
        setLastActionResult(null);
        setTraceRequestId("");
        setTraceLoading(false);
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

        if (page !== 0) {
            resetPage();
        }
    }

    function renderSortArrow(sortKey) {
        if (sortState.key !== sortKey) return "↕";
        return sortState.direction === "asc" ? "↑" : "↓";
    }

    async function reloadCurrentPage() {
        const response = await getAdminRefunds({
            status: appliedFilters.status,
            from: appliedFilters.from,
            to: appliedFilters.to,
            settlementId: anchorSettlementId || undefined,
            keyword: appliedFilters.keyword,
            sortKey: sortState.key,
            sortDirection: sortState.direction,
            page,
            size,
        });

        const items = Array.isArray(response?.items) ? response.items : [];
        setRefunds(items);
        setPageInfo({
            page: response?.page ?? page,
            size: response?.size ?? size,
            totalElements: response?.totalElements ?? items.length,
            totalPages: response?.totalPages ?? 0,
            hasNext: response?.hasNext ?? false,
            hasPrevious: response?.hasPrevious ?? false,
        });

        setSelectedRefund((prev) => {
            if (!prev?.refundId) return null;
            const matched = items.find((row) => row.refundId === prev.refundId);
            return matched || null;
        });
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
            await reloadCurrentPage();
            setSelectedRefund(null);
            setTraceRequestId(result?.requestId || "");
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
        const requestId = lastActionResult?.requestId || traceRequestId;

        if (!requestId) {
            setErrorMessage("이 환불 건의 requestId를 찾을 수 없어 Trace로 이동할 수 없습니다.");
            return;
        }

        setErrorMessage("");
        navigate(`/admin/audit?requestId=${encodeURIComponent(requestId)}`);
    }

    if (meLoading) {
        return (
            <PageLayout
                title="환불 큐"
                description="A6 환불 큐 단일 선택 승인/거절 화면입니다."
            >
                <div className="guard-notice">
                    <div className="guard-notice__title">로딩 중</div>
                    <div className="guard-notice__description">
                        권한 정보를 확인하는 중입니다.
                    </div>
                </div>
            </PageLayout>
        );
    }

    if (meErrorInfo?.status === 401) {
        return (
            <PageLayout
                title="환불 큐"
                description="A6 환불 큐 단일 선택 승인/거절 화면입니다."
            >
                <RequireLoginNotice />
            </PageLayout>
        );
    }

    if (meErrorInfo?.status === 403) {
        return (
            <PageLayout
                title="환불 큐"
                description="A6 환불 큐 단일 선택 승인/거절 화면입니다."
            >
                <div className="guard-notice">
                    <div className="guard-notice__title">접근 불가</div>
                    <div className="guard-notice__description">
                        {meErrorInfo.message}
                    </div>
                </div>
            </PageLayout>
        );
    }

    if (meErrorInfo) {
        return (
            <PageLayout
                title="환불 큐"
                description="A6 환불 큐 단일 선택 승인/거절 화면입니다."
            >
                <div className="guard-notice">
                    <div className="guard-notice__title">조회 실패</div>
                    <div className="guard-notice__description">
                        {meErrorInfo.message}
                    </div>
                </div>
            </PageLayout>
        );
    }

    return (
        <PageLayout
            title="환불 큐"
            description="A6 환불 큐 단일 선택 승인/거절 화면입니다."
        >
            <style>{`
                .refund-queue-page {
                    display: flex;
                    flex-direction: column;
                    gap: 14px;
                    width: 100%;
                    max-width: 1240px;
                    margin: 0 auto;
                    min-width: 0;
                }

                .refund-queue-stack {
                    display: flex;
                    flex-direction: column;
                    gap: 14px;
                    width: 100%;
                    min-width: 0;
                }

                .refund-queue-guide {
                    margin-bottom: 10px;
                    padding: 10px 12px;
                    border: 1px solid #d0d5dd;
                    border-radius: 10px;
                    background: #f8fafc;
                    color: #667085;
                    font-size: 12px;
                    line-height: 1.45;
                }

                .refund-queue-result {
                    margin-bottom: 10px;
                    padding: 10px 12px;
                    border: 1px solid #d0d5dd;
                    border-radius: 10px;
                    background: #f8fafc;
                }

                .refund-queue-result-title {
                    font-size: 13px;
                    font-weight: 800;
                    color: #101828;
                    margin-bottom: 6px;
                }

                .refund-queue-result-grid {
                    display: grid;
                    grid-template-columns: repeat(3, minmax(0, 1fr));
                    gap: 8px 12px;
                    margin-bottom: 10px;
                }

                .refund-queue-result-item {
                    display: flex;
                    align-items: center;
                    gap: 6px;
                    min-width: 0;
                    font-size: 13px;
                }

                .refund-queue-result-item strong {
                    color: #475467;
                    font-weight: 700;
                    flex: 0 0 auto;
                }

                .refund-queue-result-item span {
                    color: #101828;
                    min-width: 0;
                    word-break: break-word;
                }

                .refund-queue-card {
                    border: 1px solid var(--color-border);
                    border-radius: var(--radius-lg);
                    background: var(--color-surface);
                    padding: 12px;
                    overflow: hidden;
                }

                .refund-queue-detail-grid {
                    display: grid;
                    grid-template-columns: repeat(3, minmax(0, 1fr));
                    gap: 10px;
                    margin-bottom: 12px;
                }

                .refund-queue-detail-item {
                    display: flex;
                    align-items: center;
                    gap: 10px;
                    min-width: 0;
                    min-height: 60px;
                    padding: 10px 12px;
                    border: 1px solid var(--color-border);
                    border-radius: 10px;
                    background: #fcfcfd;
                }

                .refund-queue-detail-label {
                    flex: 0 0 92px;
                    font-size: 12px;
                    font-weight: 700;
                    color: #667085;
                }

                .refund-queue-detail-value {
                    min-width: 0;
                    flex: 1 1 auto;
                    font-size: 14px;
                    font-weight: 700;
                    color: #101828;
                    display: flex;
                    align-items: center;
                    gap: 8px;
                    flex-wrap: wrap;
                }

                .refund-queue-detail-value .copyable-id {
                    display: inline-grid;
                    grid-template-columns: minmax(0, 1fr) auto;
                    align-items: center;
                    gap: 8px;
                    width: 100%;
                    max-width: 100%;
                }

                .refund-queue-detail-value .copyable-id__text {
                    min-width: 0;
                    white-space: normal;
                    overflow: visible;
                    text-overflow: unset;
                    word-break: break-all;
                    overflow-wrap: anywhere;
                }

                .refund-queue-detail-value .amount-text {
                    font-size: 14px;
                }

                .refund-queue-comment-block {
                    margin-top: 0;
                }

                .refund-queue-comment-label {
                    display: block;
                    margin-bottom: 6px;
                    font-size: 13px;
                    font-weight: 800;
                    color: #344054;
                }

                .refund-queue-comment-label .required {
                    color: #d92d20;
                }

                .refund-queue-comment-textarea {
                    width: 100%;
                    min-height: 68px;
                    resize: vertical;
                    box-sizing: border-box;
                }

                .refund-queue-actions {
                    display: grid;
                    grid-template-columns: repeat(3, minmax(0, 1fr));
                    gap: 8px;
                    margin-top: 10px;
                }

                .refund-queue-meta-note {
                    margin-top: 8px;
                    font-size: 12px;
                    color: #667085;
                    line-height: 1.45;
                }

                .refund-queue-toolbar {
                    display: flex;
                    align-items: end;
                    justify-content: space-between;
                    gap: 10px;
                    margin-bottom: 10px;
                    flex-wrap: wrap;
                }

                .refund-queue-search {
                    flex: 1 1 340px;
                    max-width: 480px;
                }

                .refund-queue-search .form-field__label {
                    display: block;
                    margin-bottom: 6px;
                    font-size: 12px;
                    font-weight: 700;
                    color: #475467;
                }

                .refund-queue-search .input {
                    width: 100%;
                }

                .refund-queue-toolbar-actions {
                    display: flex;
                    gap: 8px;
                    flex: 0 0 auto;
                }

                .refund-queue-anchor-banner {
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    gap: 12px;
                    margin-bottom: 12px;
                    padding: 12px 14px;
                    border: 1px solid var(--color-border);
                    border-radius: 10px;
                    background: #f8fafc;
                }

                .refund-queue-anchor-banner strong {
                    font-size: 13px;
                    font-weight: 700;
                    color: #344054;
                }

                .refund-queue-anchor-banner span {
                    flex: 1 1 auto;
                    font-size: 13px;
                    color: #475467;
                }

                .refund-queue-table-wrap {
                    width: 100%;
                    overflow-x: auto;
                    overflow-y: hidden;
                    border: 1px solid var(--color-border);
                    border-radius: var(--radius-lg);
                    background: var(--color-surface);
                    box-sizing: border-box;
                }

                .refund-queue-table {
                    width: 100%;
                    min-width: 940px;
                    border-collapse: separate;
                    border-spacing: 0;
                    table-layout: fixed;
                }

                .refund-queue-table th,
                .refund-queue-table td {
                    padding: 9px 10px;
                    border-bottom: 1px solid var(--color-border);
                    vertical-align: middle;
                }

                .refund-queue-table thead th {
                    background: #f8fafc;
                    color: #475467;
                    font-size: 12px;
                    font-weight: 800;
                    text-align: left;
                    white-space: nowrap;
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

                .refund-queue-sort-button {
                    display: inline-flex;
                    align-items: center;
                    gap: 5px;
                    padding: 0;
                    border: 0;
                    background: transparent;
                    color: inherit;
                    font: inherit;
                    font-weight: 800;
                    cursor: pointer;
                }

                .refund-queue-sort-arrow {
                    font-size: 11px;
                    color: #667085;
                    line-height: 1;
                }

                .refund-queue-radio-col {
                    width: 58px;
                }

                .refund-queue-id-col {
                    width: 190px;
                }

                .refund-queue-merchant-col {
                    width: 140px;
                }

                .refund-queue-amount-col,
                .refund-queue-captured-col,
                .refund-queue-refundable-col {
                    width: 110px;
                }

                .refund-queue-datetime-col {
                    width: 150px;
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

                .refund-queue-table-cell-copy .copyable-id {
                    display: inline-flex;
                    align-items: center;
                    gap: 6px;
                    max-width: 100%;
                    flex-wrap: nowrap;
                    white-space: nowrap;
                }

                .refund-queue-table-cell-copy .copyable-id__text {
                    display: inline-block;
                    min-width: 0;
                    max-width: calc(100% - 56px);
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                }

                .refund-queue-note {
                    margin-top: 8px;
                    font-size: 12px;
                    color: #667085;
                }

                .refund-queue-pagination {
                    display: flex;
                    justify-content: center;
                    margin-top: 12px;
                }

                @media (max-width: 1200px) {
                    .refund-queue-detail-grid {
                        grid-template-columns: repeat(2, minmax(0, 1fr));
                    }
                }

                @media (max-width: 1080px) {
                    .refund-queue-result-grid,
                    .refund-queue-actions {
                        grid-template-columns: 1fr;
                    }

                    .refund-queue-anchor-banner {
                        flex-direction: column;
                        align-items: flex-start;
                    }
                }

                @media (max-width: 768px) {
                    .refund-queue-toolbar {
                        align-items: stretch;
                    }

                    .refund-queue-toolbar-actions {
                        width: 100%;
                    }

                    .refund-queue-toolbar-actions > * {
                        flex: 1 1 0;
                    }

                    .refund-queue-detail-grid {
                        grid-template-columns: 1fr;
                    }

                    .refund-queue-table {
                        min-width: 900px;
                    }
                }
            `}</style>

            <div className="refund-queue-page">
                {errorMessage ? <ErrorState message={errorMessage} /> : null}

                <div className="refund-queue-stack">
                    <SectionCard title="선택 상세/결정">
                        <div className="refund-queue-guide">
                            선택된 1건을 승인/거절합니다. 이미 결정된 건 재호출은
                            no-op 200(+status, decidedAt) 규격입니다.
                        </div>

                        {lastActionResult ? (
                            <div className="refund-queue-result">
                                <div className="refund-queue-result-title">처리 결과</div>

                                <div className="refund-queue-result-grid">
                                    <div className="refund-queue-result-item">
                                        <strong>refundId</strong>
                                        <span>{lastActionResult.refundId}</span>
                                    </div>
                                    <div className="refund-queue-result-item">
                                        <strong>status</strong>
                                        <span>{lastActionResult.status || "-"}</span>
                                    </div>
                                    <div className="refund-queue-result-item">
                                        <strong>requestId</strong>
                                        <span>{lastActionResult.requestId || "-"}</span>
                                    </div>
                                </div>
                            </div>
                        ) : null}

                        <div className="refund-queue-card">
                            <div className="refund-queue-detail-grid">
                                <div className="refund-queue-detail-item">
                                    <span className="refund-queue-detail-label">refundId</span>
                                    <span className="refund-queue-detail-value">
                                        {selectedRow ? <CopyableId value={selectedRow.refundId} /> : "-"}
                                    </span>
                                </div>

                                <div className="refund-queue-detail-item">
                                    <span className="refund-queue-detail-label">captured</span>
                                    <span className="refund-queue-detail-value">
                                        <AmountText value={getAmountValue(selectedRow, "capturedAmount")} />
                                    </span>
                                </div>

                                <div className="refund-queue-detail-item">
                                    <span className="refund-queue-detail-label">merchantId</span>
                                    <span className="refund-queue-detail-value">
                                        {renderText(selectedRow?.merchantId)}
                                    </span>
                                </div>

                                <div className="refund-queue-detail-item">
                                    <span className="refund-queue-detail-label">paymentId</span>
                                    <span className="refund-queue-detail-value">
                                        {selectedRow ? <CopyableId value={selectedRow.paymentId} /> : "-"}
                                    </span>
                                </div>

                                <div className="refund-queue-detail-item">
                                    <span className="refund-queue-detail-label">refund</span>
                                    <span className="refund-queue-detail-value">
                                        <AmountText
                                            value={getAmountValue(selectedRow, "amount", ["refundAmount"])}
                                        />
                                    </span>
                                </div>

                                <div className="refund-queue-detail-item">
                                    <span className="refund-queue-detail-label">status</span>
                                    <span className="refund-queue-detail-value">
                                        {selectedRow ? <StatusBadge status={selectedRow.status} /> : "-"}
                                    </span>
                                </div>

                                <div className="refund-queue-detail-item">
                                    <span className="refund-queue-detail-label">요청 메모</span>
                                    <span className="refund-queue-detail-value">
                                        {renderText(selectedRow?.reasonText)}
                                    </span>
                                </div>

                                <div className="refund-queue-detail-item">
                                    <span className="refund-queue-detail-label">refundable</span>
                                    <span className="refund-queue-detail-value">
                                        <AmountText value={getAmountValue(selectedRow, "refundableAmount")} />
                                    </span>
                                </div>

                                <div className="refund-queue-detail-item">
                                    <span className="refund-queue-detail-label">requestedAt</span>
                                    <span className="refund-queue-detail-value">
                                        {renderDate(selectedRow?.requestedAt)}
                                    </span>
                                </div>
                            </div>

                            <div className="refund-queue-comment-block">
                                <label className="refund-queue-comment-label">
                                    운영 메모(comment) <span className="required">(필수)</span>
                                </label>
                                <textarea
                                    className="textarea refund-queue-comment-textarea"
                                    value={comment}
                                    onChange={(event) => setComment(event.target.value)}
                                    placeholder="승인/거절 사유를 입력하세요."
                                    disabled={acting || selectedStatus !== "REQUESTED"}
                                />
                            </div>

                            <div className="refund-queue-actions">
                                <ActionButton
                                    type="button"
                                    variant="secondary"
                                    disabled={
                                        !selectedRow ||
                                        traceLoading ||
                                        (!lastActionResult?.requestId && !traceRequestId)
                                    }
                                    onClick={moveToTrace}
                                >
                                    {traceLoading ? "Trace 확인 중..." : "Trace로 보기(A1)"}
                                </ActionButton>

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
                                * 409 포맷: {"{`{\"code\":\"RULE_VIOLATION\",\"reason\":\"INSUFFICIENT_REFUNDABLE\"}`}"}
                            </div>
                        </div>
                    </SectionCard>

                    {anchorSettlementId ? (
                        <div className="refund-queue-anchor-banner">
                            <strong>A4 정산 상세에서 이동됨</strong>
                            <span>
                                settlementId 기준으로 연결된 환불만 표시 중{" "}
                                <CopyableId value={anchorSettlementId} short />
                            </span>
                            <ActionButton
                                type="button"
                                variant="secondary"
                                onClick={() => navigate("/admin/refunds")}
                            >
                                전체 환불 큐 보기
                            </ActionButton>
                        </div>
                    ) : null}

                    <SectionCard title="대기 큐(REQUESTED)">
                        <form onSubmit={handleSearch}>
                            <div className="refund-queue-toolbar">
                                <div className="refund-queue-search">
                                    <label className="form-field__label">
                                        키워드(환불ID/결제ID)
                                    </label>
                                    <input
                                        className="input"
                                        type="text"
                                        name="keyword"
                                        value={draftFilters.keyword}
                                        onChange={handleDraftFilterChange}
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
                        ) : refunds.length === 0 ? (
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

                                            <th className="refund-queue-merchant-col">merchantId</th>

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

                                            <th className="refund-queue-captured-col">captured</th>
                                            <th className="refund-queue-refundable-col">refundable</th>

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
                                        {refunds.map((row) => {
                                            const isSelected = row.refundId === selectedRefundId;

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
                                                                onChange={() => handleSelectRefund(row)}
                                                                onClick={(event) => event.stopPropagation()}
                                                                aria-label={`${row.refundId} 선택`}
                                                            />
                                                        </div>
                                                    </td>

                                                    <td className="refund-queue-id-col refund-queue-table-cell-copy">
                                                        <CopyableId value={row.refundId} short />
                                                    </td>

                                                    <td className="refund-queue-merchant-col">
                                                        {renderText(row.merchantId)}
                                                    </td>

                                                    <td className="refund-queue-amount-col">
                                                        <AmountText
                                                            value={getAmountValue(row, "amount", ["refundAmount"])}
                                                        />
                                                    </td>

                                                    <td className="refund-queue-captured-col">
                                                        <AmountText
                                                            value={getAmountValue(row, "capturedAmount")}
                                                        />
                                                    </td>

                                                    <td className="refund-queue-refundable-col">
                                                        <AmountText
                                                            value={getAmountValue(row, "refundableAmount")}
                                                        />
                                                    </td>

                                                    <td className="refund-queue-datetime-col">
                                                        {renderDate(row.requestedAt)}
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
        </PageLayout>
    );
}