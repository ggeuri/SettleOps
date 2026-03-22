function formatAmount(value) {
    if (value === null || value === undefined || value === "") {
        return "-";
    }

    return `${Number(value).toLocaleString("ko-KR")}원`;
}

export default function AmountText({ value }) {
    return <span className="amount-text">{formatAmount(value)}</span>;
}