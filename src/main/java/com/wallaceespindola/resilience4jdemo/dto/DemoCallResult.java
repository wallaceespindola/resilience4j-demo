package com.wallaceespindola.resilience4jdemo.dto;

import java.time.Instant;

/** Result returned by individual module demo endpoints. */
public record DemoCallResult(
        String module,
        String outcome,      // "success" | "fallback" | "rejected" | "timeout"
        String detail,
        int attemptNumber,
        long elapsedMs,
        String circuitBreakerState,
        String timestamp
) {
    public static DemoCallResult success(String module, String detail, int attempt, long elapsed, String cbState) {
        return new DemoCallResult(module, "success", detail, attempt, elapsed, cbState, Instant.now().toString());
    }

    public static DemoCallResult fallback(String module, String detail, int attempt, long elapsed, String cbState) {
        return new DemoCallResult(module, "fallback", detail, attempt, elapsed, cbState, Instant.now().toString());
    }

    public static DemoCallResult rejected(String module, String reason, long elapsed, String cbState) {
        return new DemoCallResult(module, "rejected", reason, 0, elapsed, cbState, Instant.now().toString());
    }

    public static DemoCallResult timeout(String module, String detail, long elapsed, String cbState) {
        return new DemoCallResult(module, "timeout", detail, 0, elapsed, cbState, Instant.now().toString());
    }
}
