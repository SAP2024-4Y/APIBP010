package com.hospital.common;

public record ApiResponse<T>(T data, boolean has_more, String next_cursor) {

    public static <T> ApiResponse<T> of(T data, boolean hasMore, String nextCursor) {
        return new ApiResponse<>(data, hasMore, nextCursor);
    }

    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data, false, null);
    }
}
