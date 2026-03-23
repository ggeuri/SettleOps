//admin-web/src/hooks/usePagination.js

import { useState } from "react";

export default function usePagination(initialPage = 0, initialSize = 20) {
    const [page, setPage] = useState(initialPage);
    const [size, setSize] = useState(initialSize);

    function resetPage() {
        setPage(0);
    }

    function changePage(nextPage) {
        setPage(Math.max(0, nextPage));
    }

    return {
        page,
        size,
        setPage: changePage,
        setSize,
        resetPage,
    };
}