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