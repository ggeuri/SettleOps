import { useMemo, useState } from "react";

function toDisplayValue(value) {
  if (value === null || value === undefined || value === "") {
    return "-";
  }
  return String(value);
}

function toShortValue(value) {
  const text = toDisplayValue(value);

  if (text === "-") return text;
  if (text.length <= 18) return text;

  return `${text.slice(0, 8)}…${text.slice(-6)}`;
}

export default function CopyableId({
  value,
  short = false,
  className = "",
  buttonLabel = "복사",
}) {
  const [copied, setCopied] = useState(false);

  const fullValue = useMemo(() => toDisplayValue(value), [value]);
  const displayValue = useMemo(() => {
    return short ? toShortValue(value) : fullValue;
  }, [fullValue, short, value]);

  const copyDisabled = fullValue === "-";

  const handleCopy = async () => {
    if (copyDisabled) return;

    try {
      await navigator.clipboard.writeText(fullValue);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 1200);
    } catch (error) {
      console.error("ID 복사 실패", error);
    }
  };

  return (
    <span className={`copyable-id ${className}`.trim()}>
      <span
        className={`copyable-id__text ${short ? "copyable-id__text--short" : ""}`.trim()}
        title={fullValue}
      >
        {displayValue}
      </span>

      <button
        type="button"
        className="copyable-id__button"
        onClick={handleCopy}
        disabled={copyDisabled}
        aria-label={`${fullValue} 복사`}
        title={copyDisabled ? "복사할 값이 없습니다." : `${fullValue} 복사`}
      >
        {copied ? "복사됨" : buttonLabel}
      </button>
    </span>
  );
}