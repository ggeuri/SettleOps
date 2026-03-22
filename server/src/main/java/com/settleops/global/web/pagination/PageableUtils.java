package com.settleops.global.web.pagination;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageableUtils {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private PageableUtils() {
    }

    public static Pageable normalize(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, DEFAULT_SIZE, Sort.unsorted());
        }

        int page = Math.max(0, pageable.getPageNumber());

        int size = pageable.getPageSize();
        if (size <= 0) {
            size = DEFAULT_SIZE;
        }
        if (size > MAX_SIZE) {
            size = MAX_SIZE;
        }

        return PageRequest.of(page, size, Sort.unsorted());
    }
}