export default function ErrorState({ message }) {
    return (
        <div className="state-block">
            <div className="state-block__title">오류</div>
            <div className="state-block__description">{message}</div>
        </div>
    );
}