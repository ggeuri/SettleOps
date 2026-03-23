import { copyText } from "../../utils/copyText.js";
import { showToast } from "../../utils/toast.js";

export default function CopyableId({
  value,
  short = false,
  className = "",
  disabled = false,
  title,
  buttonLabel = "복사",
  emptyText = "-",
}) {
  if (!value) {
    return <span>{emptyText}</span>;
  }

  async function handleCopy(event) {
    event.preventDefault();
    event.stopPropagation();

    if (disabled) return;

    try {
      await copyText(String(value));
      showToast("복사 완료");
    } catch (error) {
      console.error("copy failed", error);
    }
  }

  const resolvedTitle = title ?? String(value);
  const textClassName = short
    ? "copyable-id__text copyable-id__text--short"
    : "copyable-id__text";

  return (
    <span
      className={`copyable-id ${className}`.trim()}
      title={resolvedTitle}
    >
      <span className={textClassName}>{String(value)}</span>

      <button
        type="button"
        className="copyable-id__button"
        onClick={handleCopy}
        aria-label="ID 복사"
        disabled={disabled}
      >
        {buttonLabel}
      </button>
    </span>
  );
}