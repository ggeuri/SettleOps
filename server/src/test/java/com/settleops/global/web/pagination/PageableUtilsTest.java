package com.settleops.global.web.pagination;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;

class PageableUtilsTest {

    @Test
    @DisplayName("pageable이 null이면 기본 page=0 size=20으로 보정한다")
    void normalize_null() {
        Pageable normalized = PageableUtils.normalize(null);

        assertThat(normalized.getPageNumber()).isEqualTo(0);
        assertThat(normalized.getPageSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("size가 100을 초과하면 100으로 보정한다")
    void normalize_clampsMaxSize() {
        Pageable normalized = PageableUtils.normalize(PageRequest.of(0, 999));

        assertThat(normalized.getPageNumber()).isEqualTo(0);
        assertThat(normalized.getPageSize()).isEqualTo(100);
    }

    @Test
    @DisplayName("정상 page/size는 그대로 유지한다")
    void normalize_keepsValidValues() {
        Pageable normalized = PageableUtils.normalize(PageRequest.of(1, 10));

        assertThat(normalized.getPageNumber()).isEqualTo(1);
        assertThat(normalized.getPageSize()).isEqualTo(10);
    }
    
    @Test
    @DisplayName("size가 0 이하면 기본 size=20으로 보정한다")
    void normalize_nonPositiveSize() {
        Pageable normalized = PageableUtils.normalize(PageRequest.of(0, 1)).withPage(0);
    }
}