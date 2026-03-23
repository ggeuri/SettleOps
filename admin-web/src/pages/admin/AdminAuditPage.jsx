import { useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import PageLayout from "../../components/layout/PageLayout.jsx";
import SectionCard from "../../components/layout/SectionCard.jsx";
import AuditSearchForm from "../../components/audit/AuditSearchForm.jsx";
import AuditLogTable from "../../components/audit/AuditLogTable.jsx";
import AuditLogRowExpand from "../../components/audit/AuditLogRowExpand.jsx";
import { fetchAuditLogs } from "../../api/auditLogsApi.js";
import { fetchAuditEvents } from "../../api/auditEventsApi.js";

const DEFAULT_PAGE = 0;
const DEFAULT_SIZE = 20;

const EMPTY_RESULT = {
  requestId: null,
  items: [],
  page: DEFAULT_PAGE,
  size: DEFAULT_SIZE,
  totalElements: 0,
  totalPages: 0,
};

const EMPTY_EVENT_RESULT = {
  requestId: null,
  items: [],
};

function parseQuery(search) {
  const params = new URLSearchParams(search);

  return {
    requestId: params.get("requestId") || "",
    merchantId: params.get("merchantId") || "",
    entityType: params.get("entityType") || "",
    from: params.get("from") || "",
    to: params.get("to") || "",
    includeNoOp: params.get("includeNoOp") === "true",
    page: Number(params.get("page") || DEFAULT_PAGE),
    size: Number(params.get("size") || DEFAULT_SIZE),
  };
}

function buildQuery(params) {
  const searchParams = new URLSearchParams();

  if (params.requestId) searchParams.set("requestId", params.requestId);
  if (params.merchantId) searchParams.set("merchantId", params.merchantId);
  if (params.entityType) searchParams.set("entityType", params.entityType);
  if (params.from) searchParams.set("from", params.from);
  if (params.to) searchParams.set("to", params.to);
  if (params.includeNoOp) searchParams.set("includeNoOp", "true");

  searchParams.set("page", String(params.page ?? DEFAULT_PAGE));
  searchParams.set("size", String(params.size ?? DEFAULT_SIZE));

  return searchParams.toString();
}

function buildRowKey(item, index) {
  return `${item?.auditId ?? "audit"}-${index}`;
}

// 최소 수정: date 입력값을 API 호출 직전에만 LocalDateTime 문자열로 변환
function toFromDateTime(dateValue) {
  if (!dateValue) return undefined;
  return `${dateValue}T00:00:00`;
}

function toToDateTime(dateValue) {
  if (!dateValue) return undefined;
  return `${dateValue}T23:59:59`;
}

export default function AdminAuditPage() {
  const location = useLocation();
  const navigate = useNavigate();

  const queryState = useMemo(() => parseQuery(location.search), [location.search]);

  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [result, setResult] = useState(EMPTY_RESULT);
  const [selectedRowKey, setSelectedRowKey] = useState("");

  const [eventLoading, setEventLoading] = useState(false);
  const [eventErrorMessage, setEventErrorMessage] = useState("");
  const [eventResult, setEventResult] = useState(EMPTY_EVENT_RESULT);

  const hasSearchKey = Boolean(queryState.requestId || queryState.merchantId);
  const items = Array.isArray(result.items) ? result.items : [];

  useEffect(() => {
    if (!hasSearchKey) {
      setResult(EMPTY_RESULT);
      setErrorMessage("");
      setSelectedRowKey("");
      setEventResult(EMPTY_EVENT_RESULT);
      setEventErrorMessage("");
      return;
    }

    let cancelled = false;

    async function loadAuditLogs() {
      try {
        setLoading(true);
        setErrorMessage("");

        const data = await fetchAuditLogs({
          requestId: queryState.requestId || undefined,
          merchantId: queryState.merchantId || undefined,
          entityType: queryState.entityType || undefined,
          from: toFromDateTime(queryState.from),
          to: toToDateTime(queryState.to),
          includeNoOp: queryState.includeNoOp,
          page: queryState.page,
          size: queryState.size,
        });

        if (cancelled) return;

        setResult(data ?? EMPTY_RESULT);
      } catch (error) {
        if (cancelled) return;

        setErrorMessage(
          error?.body?.message || "Trace / Audit 조회 중 오류가 발생했습니다."
        );
        setResult(EMPTY_RESULT);
        setSelectedRowKey("");
        setEventResult(EMPTY_EVENT_RESULT);
        setEventErrorMessage("");
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    loadAuditLogs();

    return () => {
      cancelled = true;
    };
  }, [hasSearchKey, queryState]);

  useEffect(() => {
    if (items.length === 0) {
      setSelectedRowKey("");
      setEventResult(EMPTY_EVENT_RESULT);
      setEventErrorMessage("");
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

  useEffect(() => {
    const selectedRequestId = selectedItem?.requestId || result.requestId;

    if (!selectedItem || !selectedRequestId) {
      setEventResult(EMPTY_EVENT_RESULT);
      setEventErrorMessage("");
      return;
    }

    let cancelled = false;

    async function loadAuditEvents() {
      try {
        setEventLoading(true);
        setEventErrorMessage("");

        const data = await fetchAuditEvents(selectedRequestId);

        if (cancelled) return;
        setEventResult(data ?? EMPTY_EVENT_RESULT);
      } catch (error) {
        if (cancelled) return;

        setEventErrorMessage(
          error?.body?.message || "Audit event 조회 중 오류가 발생했습니다."
        );
        setEventResult(EMPTY_EVENT_RESULT);
      } finally {
        if (!cancelled) {
          setEventLoading(false);
        }
      }
    }

    loadAuditEvents();

    return () => {
      cancelled = true;
    };
  }, [selectedItem, result.requestId]);

  function handleSearch(formValues) {
    const nextQuery = buildQuery({
      requestId: formValues.requestId?.trim() || "",
      merchantId: formValues.merchantId?.trim() || "",
      entityType: formValues.entityType || "",
      from: formValues.from || "",
      to: formValues.to || "",
      includeNoOp: Boolean(formValues.includeNoOp),
      page: DEFAULT_PAGE,
      size: DEFAULT_SIZE,
    });

    navigate(`/admin/audit${nextQuery ? `?${nextQuery}` : ""}`);
  }

  function handlePageChange(nextPage) {
    const nextQuery = buildQuery({
      ...queryState,
      page: nextPage,
      size: queryState.size || DEFAULT_SIZE,
    });

    navigate(`/admin/audit?${nextQuery}`);
  }

  function handleSelectItem(rowKey) {
    setSelectedRowKey(rowKey);
  }

  return (
    <PageLayout
      title="Trace / Audit"
      description="A1 운영대시. requestId 또는 merchantId 기준으로 요청 단위 재현 타임라인을 조회합니다."
    >
      <SectionCard title="검색 조건">
        <AuditSearchForm
          initialValues={{
            requestId: queryState.requestId,
            merchantId: queryState.merchantId,
            entityType: queryState.entityType,
            from: queryState.from,
            to: queryState.to,
            includeNoOp: queryState.includeNoOp,
          }}
          onSearch={handleSearch}
          loading={loading}
        />
      </SectionCard>

      <SectionCard title="조회 결과">
        {!hasSearchKey && (
          <div className="info-list">
            <div>
              <strong>안내</strong> requestId 또는 merchantId를 입력해 조회해 주세요.
            </div>
            <div>
              <strong>조회 규칙</strong> 둘 다 입력 시 requestId가 우선합니다.
            </div>
            <div>
              <strong>운영 탐색</strong> merchantId 조회는 from/to 미입력 시 서버 기본 최근 7일이 적용됩니다.
            </div>
          </div>
        )}

        {hasSearchKey && errorMessage && (
          <div className="info-list">
            <div>
              <strong>오류</strong> {errorMessage}
            </div>
          </div>
        )}

        {hasSearchKey && !errorMessage && (
          <div
            style={{
              display: "grid",
              gridTemplateColumns: "minmax(0, 2fr) minmax(320px, 1fr)",
              gap: "16px",
              alignItems: "start",
            }}
          >
            <div>
              <AuditLogTable
                loading={loading}
                items={items}
                requestId={result.requestId}
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
              <AuditLogRowExpand
                item={selectedItem}
                events={eventResult.items}
                eventRequestId={eventResult.requestId}
                eventLoading={eventLoading}
                eventErrorMessage={eventErrorMessage}
              />
            </div>
          </div>
        )}
      </SectionCard>

      <SectionCard title="페이지 규칙">
        <div className="info-list">
          <div>
            <strong>역할</strong> Admin
          </div>
          <div>
            <strong>검색 기준</strong> requestId 또는 merchantId 중 1개 필수
          </div>
          <div>
            <strong>우선순위</strong> 둘 다 입력 시 requestId 우선
          </div>
          <div>
            <strong>표시 기준</strong> 6컬럼 고정, 상세는 우측 패널에서 확인
          </div>
          <div>
            <strong>no-op</strong> 기본값은 숨김, 포함 여부는 서버 조회 옵션으로 제어
          </div>
        </div>
      </SectionCard>
    </PageLayout>
  );
}