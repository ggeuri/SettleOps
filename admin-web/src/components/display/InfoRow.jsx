export default function InfoRow({ label, children }) {
    return (
        <div className="kv-item">
            <div className="kv-item__label">{label}</div>
            <div className="kv-item__value">{children}</div>
        </div>
    );
}