export default function InfoRow({
  label,
  value,
  className = "",
}) {
  const normalizedValue =
    value === null || value === undefined || value === "" ? "-" : value;

  return (
    <div className={`info-row ${className}`.trim()}>
      <div className="info-row__label">{label}</div>
      <div className="info-row__value">
        {typeof normalizedValue === "string" || typeof normalizedValue === "number"
          ? String(normalizedValue)
          : normalizedValue}
      </div>
    </div>
  );
}