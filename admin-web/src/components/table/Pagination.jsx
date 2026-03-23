//admin-web/src/components/table/Pagination.jsx

import ActionButton from "../layout/ActionButton.jsx";

export default function Pagination({
                                       page,
                                       totalPages,
                                       onPageChange,
                                       disabled = false,
                                   }) {
    if (!totalPages || totalPages <= 1) {
        return null;
    }

    const canGoPrev = page > 0;
    const canGoNext = page < totalPages - 1;
    const pages = Array.from({ length: totalPages }, (_, index) => index);

    return (
        <div className="pagination">
            <ActionButton
                type="button"
                variant="secondary"
                disabled={disabled || !canGoPrev}
                onClick={() => onPageChange(page - 1)}
            >
                이전
            </ActionButton>

            {pages.map((pageIndex) => {
                const isCurrent = pageIndex === page;

                return (
                    <ActionButton
                        key={pageIndex}
                        type="button"
                        variant={isCurrent ? "primary" : "secondary"}
                        disabled={disabled || isCurrent}
                        onClick={() => onPageChange(pageIndex)}
                    >
                        {pageIndex + 1}
                    </ActionButton>
                );
            })}

            <ActionButton
                type="button"
                variant="secondary"
                disabled={disabled || !canGoNext}
                onClick={() => onPageChange(page + 1)}
            >
                다음
            </ActionButton>
        </div>
    );
}