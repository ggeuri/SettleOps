import { useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import PageLayout from "../../components/layout/PageLayout.jsx";
import SectionCard from "../../components/layout/SectionCard.jsx";
import HoldSearchForm from "../../components/hold/HoldSearchForm.jsx";
import HoldTable from "../../components/hold/HoldTable.jsx";
import HoldDetailPanel from "../../components/hold/HoldDetailPanel.jsx";
import {
  fetchHolds,
  approveHold,
  releaseHold,
} from "../../api/holdsApi.js";

const DEFAULT_PAGE = 0;
const DEFAULT_SIZE = 20;

const EMPTY_RESULT = {
  items: [],
  page: DEFAULT_PAGE,
  size: DEFAULT_SIZE,
  totalElements: 0,
  totalPages: 0,
};

function parseQuery(search) {
  const params = new URLSearchParams(search);

  return {
    status: params.get("status") || "",
    settlementId: params.get("settlementId") || "",
    merchantId: params.get("merchantId") || "",
    page: Number(params.get("page") || DEFAULT_PAGE),
    size: Number(params.get("size") || DEFAULT_SIZE),
  };
}

function buildQuery(params) {
  const searchParams = new URLSearchParams();

  if (params.status) searchParams.set("status", params.status);
  if (params.settlementId) searchParams.set("settlementId", params.settlementId);
  if (params.merchantId) searchParams.set("merchantId", params.merchantId);

  searchParams.set("page", String(params.page ?? DEFAULT_PAGE));
  searchParams.set("size", String(params.size ?? DEFAULT_SIZE));

  return searchParams.toString();
}

function buildRowKey(item, index) {
  return `${item?.holdId ?? "hold"}-${index}`;
}

export default function AdminHoldPage() {
  const location = useLocation();
  const navigate = useNavigate();

  const queryState = useMemo(() => parseQuery(location.search), [location.search]);

  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [result, setResult] = useState(EMPTY_RESULT);
  const [selectedRowKey, setSelectedRowKey] = useState("");

  const [actionLoading, setActionLoading] = useState(false);
  const [actionErrorMessage, setActionErrorMessage] = useState("");
  const [actionSuccessMessage, setActionSuccessMessage] = useState("");

  const items = Array.isArray(result.items) ? result.items : [];

  useEffect(() => {
    let cancelled = false;

    async function loadHolds() {
      try {
        setLoading(true);
        setErrorMessage("");

        const data = await fetchHolds({
          status: queryState.status || undefined,
          settlementId: queryState.settlementId || undefined,
          merchantId: queryState.merchantId || undefined,
          page: queryState.page,
          size: queryState.size,
        });

        if (cancelled) return;
        setResult(data ?? EMPTY_RESULT);
      } catch (error) {
        if (cancelled) return;

        setErrorMessage(
          error?.body?.message || "Hold 목록 조회 중 오류가 발생했습니다."
        );
        setResult(EMPTY_RESULT);
        setSelectedRowKey("");
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    loadHolds();

    return () => {
      cancelled = true;
    };
  }, [queryState]);

  useEffect(() => {
    if (items.length === 0) {
      setSelectedRowKey("");
      return;
    }

    const exists = items.some(
      (item, index) => buildRowKey(item, index) === selectedRowKey
    );

    if (!exists) {
      setSelectedRowKey(buildRowKey(items[0], 0));
    }
  }, [items, selectedRowKey]);

  const selectedItem = useMemo(() => {
    const foundIndex = items.findIndex(
      (item, index) => buildRowKey(item, index) === selectedRowKey
    );

    if (foundIndex < 0) {
      return null;
    }

    return items[foundIndex];
  }, [items, selectedRowKey]);

  function handleSearch(formValues) {
    const nextQuery = buildQuery({
      status: formValues.status || "",
      settlementId: formValues.settlementId?.trim() || "",
      merchantId: formValues.merchantId?.trim() || "",
      page: DEFAULT_PAGE,
      size: DEFAULT_SIZE,
    });

    navigate(`/admin/holds${nextQuery ? `?${nextQuery}` : ""}`);
  }

  function handlePageChange(nextPage) {
    const nextQuery = buildQuery({
      ...queryState,
      page: nextPage,
      size: queryState.size || DEFAULT_SIZE,
    });

    navigate(`/admin/holds?${nextQuery}`);
  }

  function handleSelectItem(rowKey) {
    setSelectedRowKey(rowKey);
    setActionErrorMessage("");
    setActionSuccessMessage("");
  }

  async function reloadCurrentPage() {
    const data = await fetchHolds({
      status: queryState.status || undefined,
      settlementId: queryState.settlementId || undefined,
      merchantId: queryState.merchantId || undefined,
      page: queryState.page,
      size: queryState.size,
    });

    setResult(data ?? EMPTY_RESULT);
  }

  async function handleApprove() {
    if (!selectedItem?.holdId || actionLoading) return;

    try {
      setActionLoading(true);
      setActionErrorMessage("");
      setActionSuccessMessage("");

      await approveHold(selectedItem.holdId, "");

      await reloadCurrentPage();
      setActionSuccessMessage("Hold 승인 처리가 완료되었습니다.");
    } catch (error) {
      setActionErrorMessage(
        error?.body?.message || "Hold 승인 중 오류가 발생했습니다."
      );
    } finally {
      setActionLoading(false);
    }
  }

  async function handleRelease() {
    if (!selectedItem?.holdId || actionLoading) return;

    try {
      setActionLoading(true);
      setActionErrorMessage("");
      setActionSuccessMessage("");

      await releaseHold(selectedItem.holdId, "");

      await reloadCurrentPage();
      setActionSuccessMessage("Hold 해제 처리가 완료되었습니다.");
    } catch (error) {
      setActionErrorMessage(
        error?.body?.message || "Hold 해제 중 오류가 발생했습니다."
      );
    } finally {
      setActionLoading(false);
    }
  }

  function handleMoveSettlementDetail() {
    if (!selectedItem?.settlementId) return;
    navigate(`/admin/settlements/${selectedItem.settlementId}`);
  }

  return (
    <PageLayout
      title="Hold Queue"
      description="A5 운영통제 큐. Hold 목록을 조회하고 승인/해제를 수행합니다."
    >
      <SectionCard title="검색 조건">
        <HoldSearchForm
          initialValues={{
            status: queryState.status,
            settlementId: queryState.settlementId,
            merchantId: queryState.merchantId,
          }}
          onSearch={handleSearch}
          loading={loading}
        />
      </SectionCard>

      <SectionCard title="A5 Hold 큐 목록">
        {errorMessage && (
          <div className="info-list" style={{ marginBottom: "16px" }}>
            <div>
              <strong>오류</strong> {errorMessage}
            </div>
          </div>
        )}

        <div
          style={{
            display: "grid",
            gridTemplateColumns: "minmax(0, 2fr) minmax(320px, 1fr)",
            gap: "16px",
            alignItems: "start",
          }}
        >
          <div>
            <HoldTable
              loading={loading}
              items={items}
              page={result.page ?? DEFAULT_PAGE}
              size={result.size ?? DEFAULT_SIZE}
              totalElements={result.totalElements ?? 0}
              totalPages={result.totalPages ?? 0}
              visibleCount={items.length}
              selectedRowKey={selectedRowKey}
              onSelectItem={handleSelectItem}
              onPageChange={handlePageChange}
            />
          </div>

          <div>
            <HoldDetailPanel
              item={selectedItem}
              actionLoading={actionLoading}
              actionErrorMessage={actionErrorMessage}
              actionSuccessMessage={actionSuccessMessage}
              onApprove={handleApprove}
              onRelease={handleRelease}
              onMoveSettlementDetail={handleMoveSettlementDetail}
            />
          </div>
        </div>
      </SectionCard>

      <SectionCard title="페이지 규칙">
        <div className="info-list">
          <div>
            <strong>row 기준</strong> hold 1건 = row 1개
          </div>
          <div>
            <strong>검색 조건</strong> status / settlementId / merchantId
          </div>
          <div>
            <strong>연결 기준</strong> settlementId 기준으로 A4와 연결
          </div>
          <div>
            <strong>표시 규칙</strong> paymentId는 A5 목록 기본 컬럼에 포함하지 않음
          </div>
        </div>
      </SectionCard>
    </PageLayout>
  );
}