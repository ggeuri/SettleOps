// /admin-web/src/components/display/StatusBadge.jsx

export default function StatusBadge({ status }) {
  const normalized = String(status || "").toUpperCase();

  const variantMap = {
    DEFAULT: "default",
    READY: "primary",
    CREATED: "default",
    CAPTURED: "success",
    PAY_REQUESTED: "warning",
    PAID: "success",
    REQUESTED: "warning",
    APPROVED: "success",
    REJECTED: "danger",
    HOLD_ACTIVE: "warning",
    FAILED: "danger",
  };

  const variant = variantMap[normalized] || "default";

  return (
    <span className={`badge badge--${variant}`}>
      {normalized}
    </span>
  );
}