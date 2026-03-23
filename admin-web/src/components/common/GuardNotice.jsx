export default function GuardNotice({
  title,
  message,
  tone = "warning",
  className = "",
}) {
  const normalizedTone = ["warning", "danger", "success", "info"].includes(tone)
    ? tone
    : "warning";

  return (
    <div
      className={`guard-notice guard-notice--${normalizedTone} ${className}`.trim()}
      role={normalizedTone === "danger" ? "alert" : "status"}
    >
      {title ? <div className="guard-notice__title">{title}</div> : null}

      <div className="guard-notice__description">
        {typeof message === "string" ? <span>{message}</span> : message}
      </div>
    </div>
  );
}