import StatusBadge from "../display/StatusBadge.jsx";
import CopyableId from "../display/CopyableId.jsx";

export default function InfoRow({
                                    label,
                                    value,
                                    children,
                                    copyable = false,
                                    status = false,
                                    emptyText = "-",
                                    columns = "160px 1fr",
                                    className = "",
                                }) {
    let content = children ?? value;

    if (content === null || content === undefined || content === "") {
        content = emptyText;
    }

    if (copyable && content !== emptyText) {
        content = <CopyableId value={content} short />;
    } else if (status && content !== emptyText) {
        content = <StatusBadge status={content} />;
    }

    return (
        <div
            className={`kv-item ${className}`.trim()}
            style={{
                display: "grid",
                gridTemplateColumns: columns,
                gap: "12px",
                alignItems: "start",
                padding: "6px 0",
            }}
        >
            <div
                className="kv-item__label"
                style={{
                    color: "var(--color-text-muted, #6b7280)",
                    fontWeight: 600,
                    lineHeight: 1.5,
                }}
            >
                {label}
            </div>

            <div
                className="kv-item__value"
                style={{
                    lineHeight: 1.5,
                    minWidth: 0,
                }}
            >
                {content}
            </div>
        </div>
    );
}