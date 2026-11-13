package com.hospital.common;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class ApiResponseTest {

    @Test
    void ofWithPaginationSetsAllFields() {
        ApiResponse<List<String>> r = ApiResponse.of(List.of("a", "b"), true, "cur123");
        assertThat(r.data()).containsExactly("a", "b");
        assertThat(r.has_more()).isTrue();
        assertThat(r.next_cursor()).isEqualTo("cur123");
    }

    @Test
    void ofWithDataOnlyDefaultsToNoMorePages() {
        ApiResponse<String> r = ApiResponse.of("value");
        assertThat(r.data()).isEqualTo("value");
        assertThat(r.has_more()).isFalse();
        assertThat(r.next_cursor()).isNull();
    }

    @Test
    void ofWithHasMoreFalseAndNullCursor() {
        ApiResponse<Integer> r = ApiResponse.of(42, false, null);
        assertThat(r.has_more()).isFalse();
        assertThat(r.next_cursor()).isNull();
    }

    @Test
    void ofWithEmptyList() {
        ApiResponse<List<String>> r = ApiResponse.of(List.of(), false, null);
        assertThat(r.data()).isEmpty();
    }
}
