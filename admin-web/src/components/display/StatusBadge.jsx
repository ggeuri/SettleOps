export default function StatusBadge({ status }) {
  const normalized = String(status || "UNKNOWN").toUpperCase();

  const variantMap = {
    DEFAULT: "default",
    UNKNOWN: "default",
    NONE: "default",

    READY: "primary",
    CREATED: "default",
    CAPTURED: "success",
    CONFIRMED: "success",
    UNCONFIRMED: "default",

    PAYMENT: "primary",
    REFUND: "warning",

    PAY_REQUESTED: "warning",
    PAID: "success",

    REQUESTED: "warning",
    APPROVED: "success",
    REJECTED: "danger",

    HOLD_ACTIVE: "danger",
    HOLD_REQUESTED: "warning",
    RELEASED: "default",

    FAILED: "danger",
    FAIL: "danger",
    OK: "success",
    SKIP: "default",

    REFUND_ADJUSTMENT_PENDING: "warning",
    REFUND_APPROVED_EXISTS: "success",
  };

  const variant = variantMap[normalized] || "default";

  return (
    <span className={`badge badge--${variant}`}>
      {normalized}
    </span>
  );
}