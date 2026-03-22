export default function EmptyState({
                                       title = "조회 결과 없음",
                                       description = "조건에 맞는 데이터가 없습니다.",
                                   }) {
    return (
        <div className="state-block">
            <div className="state-block__title">{title}</div>
            <div className="state-block__description">{description}</div>
        </div>
    );
}