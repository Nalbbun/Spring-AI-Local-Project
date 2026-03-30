package ai.local.nalbbun.api.dto.common;

import java.time.Instant;
import java.util.Map;

public record ApiResponse<T>(
        boolean success,
        T data,
        ApiError error,
        Instant timestamp,
        Map<String, Object> meta
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, Instant.now(), Map.of());
    }
    public static <T> ApiResponse<T> ok(T data, Map<String, Object> meta) {
        return new ApiResponse<>(true, data, null, Instant.now(), meta == null ? Map.of() : meta);
    }
    public static <T> ApiResponse<T> fail(String code, String message) {
        return new ApiResponse<>(false, null, new ApiError(code, message), Instant.now(), Map.of());
    }
}
