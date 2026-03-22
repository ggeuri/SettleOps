package com.settleops.global.web.pagination;

import com.settleops.global.error.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageableUtils {

    // pagination validation policy:
    // page >= 0
    // 1 <= size <= 100
    // invalid input -> 400 (BadRequestException)

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private PageableUtils() {
    }

    public static Pageable validateAndCreate(Integer page, Integer size) {
        int resolvedPage = (page == null) ? DEFAULT_PAGE : page;
        int resolvedSize = (size == null) ? DEFAULT_SIZE : size;

        if (resolvedPage < 0) {
            throw new BadRequestException("page must be >= 0");
        }

        if (resolvedSize <= 0) {
            throw new BadRequestException("size must be > 0");
        }

        if (resolvedSize > MAX_SIZE) {
            throw new BadRequestException("size must be <= " + MAX_SIZE);
        }

        return PageRequest.of(resolvedPage, resolvedSize, Sort.unsorted());
    }
}