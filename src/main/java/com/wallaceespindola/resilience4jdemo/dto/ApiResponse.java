package com.wallaceespindola.resilience4jdemo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/**
 * Unified API response wrapper. Every endpoint returns this record so that
 * timestamp and correlationId are always present per project standards.
 *
 * @param <T> payload type
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        T data,
        String status,
        String message,
        String timestamp,
        String correlationId,
        String path
) {
    public static <T> ApiResponse<T> ok(T data, String correlationId, String path) {
        return new ApiResponse<>(data, "success", null,
                Instant.now().toString(), correlationId, path);
    }

    public static <T> ApiResponse<T> ok(T data, String message, String correlationId, String path) {
        return new ApiResponse<>(data, "success", message,
                Instant.now().toString(), correlationId, path);
    }

    public static ApiResponse<Void> error(String message, String correlationId, String path) {
        return new ApiResponse<>(null, "error", message,
                Instant.now().toString(), correlationId, path);
    }
}
