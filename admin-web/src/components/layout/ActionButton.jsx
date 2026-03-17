export default function ActionButton({
                                         type = "button",
                                         variant = "primary",
                                         disabled = false,
                                         children,
                                         onClick,
                                     }) {
    return (
        <button
            type={type}
            className={`btn btn-${variant}`}
            disabled={disabled}
            onClick={onClick}
        >
            {children}
        </button>
    );
}