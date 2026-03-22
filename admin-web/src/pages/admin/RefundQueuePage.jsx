import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import PageLayout from "../../components/layout/PageLayout.jsx";
import SectionCard from "../../components/layout/SectionCard.jsx";
import ActionButton from "../../components/layout/ActionButton.jsx";
import StatusBadge from "../../components/display/StatusBadge.jsx";
import CopyableId from "../../components/display/CopyableId.jsx";
import AmountText from "../../components/display/AmountText.jsx";
import InfoRow from "../../components/display/InfoRow.jsx";
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

const INITIAL_FILTERS = {
    status: "",
    from: "",
    to: "",
};

export default function RefundQueuePage() {
    const [filters, setFilters] = useState(INITIAL_FILTERS);
    const [refunds, setRefunds] = useState([]);
    const [comments, setComments] = useState({});
    const [loading, setLoading] = useState(false);
    const [actingId, setActingId] = useState("");
    const [errorMessage, setErrorMessage] = useState("");
    const [lastResult, setLastResult] = useState(null);
    const [pageInfo, setPageInfo] = useState({
        page: 0,
        size: 10,
        totalElements: 0,
        totalPages: 0,
        hasNext: false,
        hasPrevious: false,
    });

    const { page, size, setPage, resetPage } = usePagination(0, 10);
    const navigate = useNavigate();

    useEffect(() => {
        // 검색 조건 변경만으로는 자동 조회하지 않고,
        // page/size 변경 시에만 조회한다.
        loadRefunds(page);
    }, [page, size]);

    function handleFilterChange(event) {
        const { name, value } = event.target;
        setFilters((prev) => ({
            ...prev,
            [name]: value,
        }));
    }

    function handleCommentChange(refundId, value) {
        setComments((prev) => ({
            ...prev,
            [refundId]: value,
        }));
    }

    async function loadRefunds(targetPage = 0) {
        setLoading(true);
        setErrorMessage("");

        try {
            const response = await getAdminRefunds({
                ...filters,
                page: targetPage,
                size,
            });

            setRefunds(Array.isArray(response?.items) ? response.items : []);
            setPageInfo({
                page: response?.page ?? 0,
                size: response?.size ?? size,
                totalElements: response?.totalElements ?? 0,
                totalPages: response?.totalPages ?? 0,
                hasNext: response?.hasNext ?? false,
                hasPrevious: response?.hasPrevious ?? false,
            });
        } catch (error) {
            setErrorMessage(error?.body?.message || "환불 큐 조회에 실패했습니다.");
        } finally {
            setLoading(false);
        }
    }

    function handleSearch(event) {
        event.preventDefault();
        setLastResult(null);
        setErrorMessage("");

        if (page !== 0) {
            resetPage();
            return;
        }

        loadRefunds(0);
    }

    async function handleAction(refundId, actionType) {
        const comment = (comments[refundId] || "").trim();
        const target = refunds.find((row) => row.refundId === refundId);

        if (!target || target.status !== "REQUESTED") {
            setErrorMessage("REQUESTED 상태에서만 승인/거절할 수 있습니다.");
            return;
        }

        if (!comment) {
            setErrorMessage("approve/reject에는 comment가 필수입니다.");
            return;
        }

        setErrorMessage("");
        setLastResult(null);
        setActingId(refundId);

        try {
            const result =
                actionType === "approve"
                    ? await approveRefund(refundId, comment)
                    : await rejectRefund(refundId, comment);

            setLastResult({
                refundId,
                actionType,
                status: result.status,
                decidedAt: result.decidedAt,
                requestId: result.requestId,
                comment,
            });

            setComments((prev) => ({
                ...prev,
                [refundId]: "",
            }));

            await loadRefunds(page);
        } catch (error) {
            if (error?.body?.reason) {
                setErrorMessage(`환불 처리 실패: ${error.body.reason}`);
            } else {
                setErrorMessage(error?.body?.message || "환불 처리에 실패했습니다.");
            }
        } finally {
            setActingId("");
        }
    }

    function handleMoveToTrace(requestId) {
        if (!requestId) return;
        navigate(`/admin/audit?requestId=${encodeURIComponent(requestId)}`);
    }

    return (
        <PageLayout
            title="환불 큐"
            description="관리자 환불 승인/거절 처리 화면입니다."
        >
            <SectionCard title="검색 조건">
                <form className="form-stack" onSubmit={handleSearch}>
                    <div className="filter-bar">
                        <div className="form-field">
                            <label className="form-field__label">status</label>
                            <select
                                className="select"
                                name="status"
                                value={filters.status}
                                onChange={handleFilterChange}
                            >
                                <option value="">전체</option>
                                <option value="REQUESTED">REQUESTED</option>
                                <option value="APPROVED">APPROVED</option>
                                <option value="REJECTED">REJECTED</option>
                            </select>
                        </div>

                        <div className="form-field">
                            <label className="form-field__label">from</label>
                            <input
                                className="input"
                                type="date"
                                name="from"
                                value={filters.from}
                                onChange={handleFilterChange}
                            />
                        </div>

                        <div className="form-field">
                            <label className="form-field__label">to</label>
                            <input
                                className="input"
                                type="date"
                                name="to"
                                value={filters.to}
                                onChange={handleFilterChange}
                            />
                        </div>
                    </div>

                    <div className="button-row action-panel">
                        <ActionButton type="submit" disabled={loading}>
                            {loading ? "조회 중..." : "조회"}
                        </ActionButton>
                    </div>
                </form>
            </SectionCard>

            {errorMessage ? (
                <SectionCard title="오류">
                    <ErrorState message={errorMessage} />
                </SectionCard>
            ) : null}

            {lastResult ? (
                <SectionCard title="최근 처리 결과">
                    <div className="kv-list">
                        <InfoRow label="refundId">
                            <CopyableId value={lastResult.refundId} />
                        </InfoRow>
                        <InfoRow label="action">{lastResult.actionType}</InfoRow>
                        <InfoRow label="status">
                            <StatusBadge status={lastResult.status} />
                        </InfoRow>
                        <InfoRow label="decidedAt">
                            {lastResult.decidedAt || "-"}
                        </InfoRow>
                        <InfoRow label="comment">
                            {lastResult.comment || "-"}
                        </InfoRow>
                        <InfoRow label="requestId">
                            <CopyableId value={lastResult.requestId} />
                        </InfoRow>
                    </div>

                    {lastResult.requestId ? (
                        <div className="button-row action-panel" style={{ marginTop: "16px" }}>
                            <ActionButton
                                type="button"
                                variant="secondary"
                                onClick={() => handleMoveToTrace(lastResult.requestId)}
                            >
                                Trace로 보기
                            </ActionButton>
                        </div>
                    ) : null}
                </SectionCard>
            ) : null}

            <SectionCard title="환불 목록">
                {loading ? (
                    <LoadingBlock
                        title="로딩 중"
                        description="환불 큐를 불러오고 있습니다."
                    />
                ) : refunds.length === 0 ? (
                    <EmptyState
                        title="조회 결과 없음"
                        description="조건에 맞는 환불이 없습니다."
                    />
                ) : (
                    <>
                        <div className="table-wrap">
                            <table className="data-table">
                                <thead>
                                <tr>
                                    <th>refundId</th>
                                    <th>paymentId</th>
                                    <th>merchantId</th>
                                    <th>amount</th>
                                    <th>status</th>
                                    <th>requestedAt</th>
                                    <th>decidedAt</th>
                                    <th>comment</th>
                                    <th>action</th>
                                </tr>
                                </thead>
                                <tbody>
                                {refunds.map((row) => {
                                    const isRequested = row.status === "REQUESTED";
                                    const disabled = actingId === row.refundId;
                                    const actionDisabled = disabled || !isRequested;

                                    return (
                                        <tr key={row.refundId}>
                                            <td><CopyableId value={row.refundId} short /></td>
                                            <td><CopyableId value={row.paymentId} short /></td>
                                            <td>{row.merchantId}</td>
                                            <td><AmountText value={row.amount} /></td>
                                            <td><StatusBadge status={row.status} /></td>
                                            <td>{row.requestedAt || "-"}</td>
                                            <td>{row.decidedAt || "-"}</td>
                                            <td className="table-cell--comment">
                                                {isRequested ? (
                                                    <textarea
                                                        className="textarea"
                                                        value={comments[row.refundId] || ""}
                                                        onChange={(event) =>
                                                            handleCommentChange(row.refundId, event.target.value)
                                                        }
                                                        placeholder="comment 입력"
                                                    />
                                                ) : (
                                                    <span className="badge badge--default">COMMENT LOCKED</span>
                                                )}
                                            </td>
                                            <td className="table-cell--actions">
                                                {isRequested ? (
                                                    <div className="button-row">
                                                        <ActionButton
                                                            type="button"
                                                            disabled={actionDisabled}
                                                            onClick={() => handleAction(row.refundId, "approve")}
                                                        >
                                                            {disabled ? "처리 중..." : "승인"}
                                                        </ActionButton>
                                                        <ActionButton
                                                            type="button"
                                                            variant="danger"
                                                            disabled={actionDisabled}
                                                            onClick={() => handleAction(row.refundId, "reject")}
                                                        >
                                                            {disabled ? "처리 중..." : "거절"}
                                                        </ActionButton>
                                                    </div>
                                                ) : (
                                                    <span className="badge badge--default">DONE</span>
                                                )}
                                            </td>
                                        </tr>
                                    );
                                })}
                                </tbody>
                            </table>
                        </div>

                        <Pagination
                            page={pageInfo.page}
                            totalPages={pageInfo.totalPages}
                            onPageChange={setPage}
                            disabled={loading}
                        />
                    </>
                )}
            </SectionCard>
        </PageLayout>
    );
}