export default function LoadingBlock({
                                         title = "로딩 중",
                                         description = "데이터를 불러오고 있습니다.",
                                     }) {
    return (
        <div className="state-block">
            <div className="state-block__title">{title}</div>
            <div className="state-block__description">{description}</div>
        </div>
    );
}