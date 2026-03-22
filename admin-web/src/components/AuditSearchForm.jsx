import { useEffect, useState } from "react";

const EMPTY_FORM = {
  requestId: "",
  merchantId: "",
  entityType: "",
  from: "",
  to: "",
  includeNoOp: false,
};

export default function AuditSearchForm({
  initialValues = EMPTY_FORM,
  onSearch,
  loading = false,
}) {
  const [form, setForm] = useState({
    requestId: initialValues.requestId || "",
    merchantId: initialValues.merchantId || "",
    entityType: initialValues.entityType || "",
    from: initialValues.from || "",
    to: initialValues.to || "",
    includeNoOp: Boolean(initialValues.includeNoOp),
  });

  useEffect(() => {
    setForm({
      requestId: initialValues.requestId || "",
      merchantId: initialValues.merchantId || "",
      entityType: initialValues.entityType || "",
      from: initialValues.from || "",
      to: initialValues.to || "",
      includeNoOp: Boolean(initialValues.includeNoOp),
    });
  }, [
    initialValues.requestId,
    initialValues.merchantId,
    initialValues.entityType,
    initialValues.from,
    initialValues.to,
    initialValues.includeNoOp,
  ]);

  function handleChange(event) {
    const { name, value, type, checked } = event.target;

    setForm((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : value,
    }));
  }

  function handleSubmit(event) {
    event.preventDefault();

    onSearch?.({
      requestId: form.requestId.trim(),
      merchantId: form.merchantId.trim(),
      entityType: form.entityType,
      from: form.from,
      to: form.to,
      includeNoOp: form.includeNoOp,
    });
  }

  function handleReset() {
    setForm(EMPTY_FORM);
    onSearch?.(EMPTY_FORM);
  }

  return (
    <form onSubmit={handleSubmit}>
      <div className="filter-bar">
        <div className="form-field search-field">
          <label className="form-field__label" htmlFor="requestId">
            Request ID
          </label>
          <input
            id="requestId"
            name="requestId"
            className="input"
            type="text"
            value={form.requestId}
            onChange={handleChange}
            placeholder="UUID"
            disabled={loading}
          />
        </div>

        <div className="form-field search-field">
          <label className="form-field__label" htmlFor="merchantId">
            Merchant ID
          </label>
          <input
            id="merchantId"
            name="merchantId"
            className="input"
            type="text"
            value={form.merchantId}
            onChange={handleChange}
            placeholder="mrc_1001"
            disabled={loading}
          />
        </div>

        <div className="form-field">
          <label className="form-field__label" htmlFor="entityType">
            Entity Type
          </label>
          <select
            id="entityType"
            name="entityType"
            className="select"
            value={form.entityType}
            onChange={handleChange}
            disabled={loading}
          >
            <option value="">전체</option>
            <option value="PAYMENT">PAYMENT</option>
            <option value="BATCH">BATCH</option>
            <option value="SETTLEMENT">SETTLEMENT</option>
            <option value="HOLD">HOLD</option>
            <option value="REFUND">REFUND</option>
          </select>
        </div>

        <div className="form-field">
          <label className="form-field__label" htmlFor="from">
            From
          </label>
          <input
            id="from"
            name="from"
            className="input input--date"
            type="date"
            value={form.from}
            onChange={handleChange}
            disabled={loading}
          />
        </div>

        <div className="form-field">
          <label className="form-field__label" htmlFor="to">
            To
          </label>
          <input
            id="to"
            name="to"
            className="input input--date"
            type="date"
            value={form.to}
            onChange={handleChange}
            disabled={loading}
          />
        </div>
      </div>

      <div
        className="table-toolbar"
        style={{ marginTop: "12px", alignItems: "center", gap: "12px" }}
      >
        <label
          style={{
            display: "inline-flex",
            alignItems: "center",
            gap: "8px",
            fontSize: "14px",
          }}
        >
          <input
            type="checkbox"
            name="includeNoOp"
            checked={form.includeNoOp}
            onChange={handleChange}
            disabled={loading}
          />
          no-op 포함
        </label>

        <div style={{ color: "#667085", fontSize: "13px" }}>
          requestId 또는 merchantId 중 1개 필수, 둘 다 입력 시 requestId 우선
        </div>
      </div>

      <div className="button-row action-panel" style={{ marginTop: "16px" }}>
        <button type="submit" className="btn btn--primary" disabled={loading}>
          {loading ? "조회 중..." : "조회"}
        </button>
        <button
          type="button"
          className="btn btn--secondary"
          onClick={handleReset}
          disabled={loading}
        >
          초기화
        </button>
      </div>
    </form>
  );
}