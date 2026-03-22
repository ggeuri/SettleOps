/**
 * TODO:
 * 현재 CopyableId는 표시 전용이며 실제 복사 기능은 구현되어 있지 않음.
 * naming 상 혼선을 줄이기 위해 후속 PR에서 clipboard 복사 기능을 추가할 예정.
 */
export default function CopyableId({
                                       value,
                                       short = false,
                                   }) {
    if (!value) {
        return <span>-</span>;
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
        </span>
    );
}