export default function PageLayout({ title, description, children }) {
    return (
        <div className="page-layout">
            <div className="page-header">
                <h1>{title}</h1>
                {description ? <p>{description}</p> : null}
            </div>
            <div className="page-content">{children}</div>
        </div>
    );
}