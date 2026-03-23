import { useEffect, useState } from "react";

const EMPTY_FORM = {
  status: "",
  settlementId: "",
  merchantId: "",
};

export default function HoldSearchForm({
  initialValues = EMPTY_FORM,
  onSearch,
  loading = false,
}) {
  const [form, setForm] = useState({
    status: initialValues.status || "",
    settlementId: initialValues.settlementId || "",
    merchantId: initialValues.merchantId || "",
  });

  useEffect(() => {
    setForm({
      status: initialValues.status || "",
      settlementId: initialValues.settlementId || "",
      merchantId: initialValues.merchantId || "",
    });
  }, [
    initialValues.status,
    initialValues.settlementId,
    initialValues.merchantId,
  ]);

  function handleChange(event) {
    const { name, value } = event.target;

    setForm((prev) => ({
      ...prev,
      [name]: value,
    }));
  }

  function handleSubmit(event) {
    event.preventDefault();

    onSearch?.({
      status: form.status,
      settlementId: form.settlementId.trim(),
      merchantId: form.merchantId.trim(),
    });
  }

  function handleReset() {
    setForm(EMPTY_FORM);
    onSearch?.(EMPTY_FORM);
  }

  return (
    <form onSubmit={handleSubmit}>
      <div className="filter-bar">
        <div className="form-field">
          <label className="form-field__label" htmlFor="status">
            Status
          </label>
          <select
            id="status"
            name="status"
            className="select"
            value={form.status}
            onChange={handleChange}
            disabled={loading}
          >
            <option value="">전체</option>
            <option value="HOLD_REQUESTED">HOLD_REQUESTED</option>
            <option value="HOLD_ACTIVE">HOLD_ACTIVE</option>
            <option value="RELEASED">RELEASED</option>
          </select>
        </div>

        <div className="form-field search-field">
          <label className="form-field__label" htmlFor="settlementId">
            Settlement ID
          </label>
          <input
            id="settlementId"
            name="settlementId"
            className="input"
            type="text"
            value={form.settlementId}
            onChange={handleChange}
            placeholder="set_5502"
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
            placeholder="m_1001"
            disabled={loading}
          />
        </div>
      </div>

      <div className="table-toolbar" style={{ marginTop: "12px" }}>
        <div style={{ color: "#667085", fontSize: "13px" }}>
          A5 Hold 큐는 hold 1건 = row 1개 기준이며, 기본 필터는 status /
          settlementId / merchantId를 사용합니다.
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