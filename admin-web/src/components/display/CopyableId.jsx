import { copyText } from "../../utils/copyText.js";
import { showToast } from "../../utils/toast.js";

export default function CopyableId({
                                       value,
                                       short = false,
                                   }) {
    if (!value) {
        return <span>-</span>;
    }

    async function handleCopy(event) {
        event.preventDefault();
        event.stopPropagation();

        try {
            await copyText(value);
            showToast("복사 완료");
        } catch (error) {
            console.error("copy failed", error);
        }
    }

    return (
        <span className="copyable-id" title={value}>
            <span
                className={
                    short
                        ? "copyable-id__text copyable-id__text--short"
                        : "copyable-id__text"
                }
            >
                {value}
            </span>

            <button
                type="button"
                className="copyable-id__button"
                onClick={handleCopy}
                aria-label="ID 복사"
            >
                복사
            </button>
        </span>
    );
}