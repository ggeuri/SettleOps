export default function SectionCard({ title, children }) {
    return (
        <section className="section-card">
            {title ? <h2 className="section-card__title">{title}</h2> : null}
            {children}
        </section>
    );
}