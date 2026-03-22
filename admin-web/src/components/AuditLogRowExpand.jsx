// src/components/AuditLogRowExpand.jsx
function formatDateTime(value) {
  if (!value) return "-";

  try {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;

    return new Intl.DateTimeFormat("ko-KR", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
      hour12: false,
    }).format(date);
  } catch {
    return value;
  }
}

function prettyJson(value) {
  if (!value) return "{}";

  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
}

function renderStatusChange(before, after) {
  const left = before || "-";
  const right = after || "-";
  return `${left} → ${right}`;
}

function EventRow({ event }) {
  return (
    <div
      style={{
        border: "1px solid #e5e7eb",
        borderRadius: "10px",
        padding: "12px",
        background: "#fff",
      }}
    >
      <div
        style={{
          display: "grid",
          gap: "6px",
        }}
      >
        <div>
          <strong>시각</strong> {formatDateTime(event?.occurredAt)}
        </div>
        <div>
          <strong>이벤트</strong> {event?.eventType || "-"}
        </div>
        <div>
          <strong>엔티티</strong> {event?.entityType || "-"}
        </div>
        <div>
          <strong>엔티티 ID</strong> {event?.entityId || "-"}
        </div>
        <div>
          <strong>상태 변경</strong>{" "}
          {renderStatusChange(event?.statusBefore, event?.statusAfter)}
        </div>
      </div>

      <details style={{ marginTop: "10px" }}>
        <summary style={{ cursor: "pointer" }}>metaJson 보기</summary>
        <pre
          style={{
            marginTop: "8px",
            padding: "12px",
            background: "#f8fafc",
            borderRadius: "8px",
            overflowX: "auto",
            fontSize: "12px",
            lineHeight: 1.5,
            whiteSpace: "pre-wrap",
            wordBreak: "break-word",
          }}
        >
          {prettyJson(event?.metaJson)}
        </pre>
      </details>
    </div>
  );
}

export default function AuditLogRowExpand({
  item,
  events = [],
  eventRequestId,
  eventLoading = false,
  eventErrorMessage = "",
}) {
  if (!item) {
    return (
      <div
        style={{
          border: "1px solid #e5e7eb",
          borderRadius: "12px",
          padding: "16px",
          background: "#fff",
        }}
      >
        <strong>상세 패널</strong>
        <div style={{ marginTop: "8px", color: "#6b7280" }}>
          좌측에서 Trace row를 선택해 주세요.
        </div>
      </div>
    );
  }

  return (
    <div
      style={{
        display: "grid",
        gap: "16px",
      }}
    >
      <section
        style={{
          border: "1px solid #e5e7eb",
          borderRadius: "12px",
          padding: "16px",
          background: "#fff",
        }}
      >
        <div style={{ marginBottom: "12px" }}>
          <strong>행위 상세</strong>
        </div>

        <div
          style={{
            display: "grid",
            gap: "8px",
          }}
        >
          <div>
            <strong>requestId</strong> {item?.requestId || "-"}
          </div>
          <div>
            <strong>occurredAt</strong> {formatDateTime(item?.occurredAt)}
          </div>
          <div>
            <strong>actorType</strong> {item?.actorType || "-"}
          </div>
          <div>
            <strong>actorId</strong> {item?.actorId || "-"}
          </div>
          <div>
            <strong>action</strong> {item?.action || "-"}
          </div>
          <div>
            <strong>entityType</strong> {item?.entityType || "-"}
          </div>
          <div>
            <strong>entityId</strong> {item?.entityId || "-"}
          </div>
          <div>
            <strong>merchantId</strong> {item?.merchantId || "-"}
          </div>
          <div>
            <strong>auditId</strong> {item?.auditId ?? "-"}
          </div>
          <div>
            <strong>statusChange</strong>{" "}
            {renderStatusChange(item?.statusBefore, item?.statusAfter)}
          </div>
        </div>

        <details style={{ marginTop: "12px" }}>
          <summary style={{ cursor: "pointer" }}>audit metaJson 보기</summary>
          <pre
            style={{
              marginTop: "8px",
              padding: "12px",
              background: "#f8fafc",
              borderRadius: "8px",
              overflowX: "auto",
              fontSize: "12px",
              lineHeight: 1.5,
              whiteSpace: "pre-wrap",
              wordBreak: "break-word",
            }}
          >
            {prettyJson(item?.metaJson)}
          </pre>
        </details>
      </section>

      <section
        style={{
          border: "1px solid #e5e7eb",
          borderRadius: "12px",
          padding: "16px",
          background: "#fff",
        }}
      >
        <div style={{ marginBottom: "12px" }}>
          <strong>관련 이벤트</strong>
          <div style={{ marginTop: "6px", color: "#6b7280", fontSize: "14px" }}>
            requestId: {eventRequestId || item?.requestId || "-"}
          </div>
        </div>

        {eventLoading && (
          <div style={{ color: "#6b7280" }}>이벤트를 조회 중입니다.</div>
        )}

        {!eventLoading && eventErrorMessage && (
          <div style={{ color: "#b91c1c" }}>{eventErrorMessage}</div>
        )}

        {!eventLoading && !eventErrorMessage && events.length === 0 && (
          <div style={{ color: "#6b7280" }}>
            연결된 payment / hold / refund 이벤트가 없습니다.
          </div>
        )}

        {!eventLoading && !eventErrorMessage && events.length > 0 && (
          <div
            style={{
              display: "grid",
              gap: "12px",
            }}
          >
            {events.map((event, index) => (
              <EventRow
                key={`${event?.entityType ?? "event"}-${event?.entityId ?? index}-${event?.occurredAt ?? index}`}
                event={event}
              />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}