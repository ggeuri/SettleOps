import { useEffect, useState } from "react";
import PageLayout from "../../components/layout/PageLayout.jsx";
import SectionCard from "../../components/layout/SectionCard.jsx";
import ActionButton from "../../components/layout/ActionButton.jsx";
import StatusBadge from "../../components/display/StatusBadge.jsx";
import { useNavigate } from "react-router-dom";
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
    const navigate = useNavigate();

    useEffect(() => {
        loadRefunds();
    }, []);

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

    async function loadRefunds() {
        setLoading(true);
        setErrorMessage("");

        try {
            const response = await getAdminRefunds(filters);
            setRefunds(Array.isArray(response) ? response : []);
        } catch (error) {
            setErrorMessage(error?.body?.message || "환불 큐 조회에 실패했습니다.");
        } finally {
            setLoading(false);
        }
    }

    async function handleSearch(event) {
        event.preventDefault();
        await loadRefunds();
    }

    async function handleAction(refundId, actionType) {
        const comment = (comments[refundId] || "").trim();
        const target = refunds.find((row) => row.refundId === refundId);
        if (!target || target.status !== "REQUESTED") {
            setErrorMessage("REQUESTED 상태에서만 승인/거절할 수 있습니다.");
            return;
        }
        setErrorMessage("");
        setLastResult(null);

        if (!comment) {
            setErrorMessage("approve/reject에는 comment가 필수입니다.");
            return;
        }

        setActingId(refundId);

        try {
            const result =
                actionType === "approve"
                    ? await approveRefund(refundId, comment)
                    : await rejectRefund(refundId, comment);

            setRefunds((prev) =>
                prev.map((row) =>
                    row.refundId === refundId
                        ? {
                            ...row,
                            status: result.status,
                            decidedAt: result.decidedAt,
                        }
                        : row
                )
            );

            setLastResult({
                refundId,
                actionType,
                status: result.status,
                decidedAt: result.decidedAt,
                requestId: result.requestId,
                comment,
            });
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
            <SectionCard>
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

                    <div className="button-row">
                        <ActionButton type="submit" disabled={loading}>
                            {loading ? "조회 중..." : "조회"}
                        </ActionButton>
                    </div>
                </form>
            </SectionCard>

            {lastResult ? (
                <SectionCard>
                    <div className="info-list">
                        <div><strong>refundId</strong> {lastResult.refundId}</div>
                        <div><strong>action</strong> {lastResult.actionType}</div>
                        <div><strong>status</strong> <StatusBadge status={lastResult.status} /></div>
                        <div><strong>decidedAt</strong> {lastResult.decidedAt || "-"}</div>
                        <div><strong>comment</strong> {lastResult.comment || "-"}</div>
                        <div>
                            <strong>requestId</strong>{" "}
                            {lastResult.requestId ? (
                                <span className="copyable-id">
                                    <span className="copyable-id__text">{lastResult.requestId}</span>
                                </span>
                            ) : ("-")}
                        </div>
                    </div>

                    {lastResult.requestId ? (
                        <div className="button-row">
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

            <SectionCard>
                {loading ? (
                    <div className="state-block">
                        <div className="state-block__title">로딩 중</div>
                        <div className="state-block__description">
                            환불 큐를 불러오고 있습니다.
                        </div>
                    </div>
                ) : refunds.length === 0 ? (
                    <div className="state-block">
                        <div className="state-block__title">조회 결과 없음</div>
                        <div className="state-block__description">
                            조건에 맞는 환불이 없습니다.
                        </div>
                    </div>
                ) : (
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

                                        <td>{row.merchantId}</td>
                                        <td>{row.amount}</td>
                                        <td>
                                            <StatusBadge status={row.status} />
                                        </td>
                                        <td>{row.requestedAt || "-"}</td>
                                        <td>{row.decidedAt || "-"}</td>

                                        <td style={{ minWidth: "220px" }}>
                                          <textarea
                                              className="textarea"
                                              value={comments[row.refundId] || ""}
                                              onChange={(event) =>
                                                  handleCommentChange(row.refundId, event.target.value)
                                              }
                                              placeholder={isRequested ? "comment 입력" : "처리 완료 상태"}
                                              disabled={!isRequested}
                                          />
                                        </td>

                                        <td>
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
                                                <span>처리 완료</span>
                                            )}
                                        </td>
                                    </tr>
                                );
                            })}
                            </tbody>
                        </table>
                    </div>
                )}
            </SectionCard>
        </PageLayout>
    );
}