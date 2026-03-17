export default function StatusBadge({ status }) {
    return <span className={`status-badge status-${String(status).toLowerCase()}`}>{status}</span>;
}