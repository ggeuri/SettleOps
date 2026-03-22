package com.settleops.global.web.pagination;

import com.settleops.global.error.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageableUtilsTest {

    @Test
    @DisplayName("page/size가 null이면 기본값 page=0 size=20을 적용한다")
    void validateAndCreate_null_usesDefaults() {
        Pageable pageable = PageableUtils.validateAndCreate(null, null);

        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("정상 page/size는 그대로 유지한다")
    void validateAndCreate_keepsValidValues() {
        Pageable pageable = PageableUtils.validateAndCreate(1, 10);

        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("page가 음수면 400 예외를 던진다")
    void validateAndCreate_rejectsNegativePage() {
        assertThatThrownBy(() -> PageableUtils.validateAndCreate(-1, 10))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("page must be >= 0");
    }

    @Test
    @DisplayName("size가 0 이하이면 400 예외를 던진다")
    void validateAndCreate_rejectsNonPositiveSize() {
        assertThatThrownBy(() -> PageableUtils.validateAndCreate(0, 0))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("size must be > 0");
    }

    @Test
    @DisplayName("size가 최대 허용값을 초과하면 400 예외를 던진다")
    void validateAndCreate_rejectsTooLargeSize() {
        assertThatThrownBy(() -> PageableUtils.validateAndCreate(0, 101))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("size must be <= 100");
    }
}