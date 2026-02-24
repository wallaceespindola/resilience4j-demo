package com.wallaceespindola.resilience4jdemo.dto;

import java.time.Instant;

/** Snapshot of all R4J module metrics for the dashboard. */
public record ResilienceMetricsDto(
        CircuitBreakerMetrics circuitBreaker,
        RetryMetrics retry,
        RateLimiterMetrics rateLimiter,
        BulkheadMetrics bulkhead,
        CacheMetrics cache,
        String timestamp
) {
    public record CircuitBreakerMetrics(
            String state,
            float failureRate,
            float slowCallRate,
            long notPermittedCalls,
            long successfulCalls,
            long failedCalls
    ) {}

    public record RetryMetrics(
            long successfulCallsWithRetry,
            long successfulCallsWithoutRetry,
            long failedCallsWithRetry,
            long failedCallsWithoutRetry
    ) {}

    public record RateLimiterMetrics(
            int availablePermissions,
            long waitingThreads
    ) {}

    public record BulkheadMetrics(
            int availableConcurrentCalls,
            int maxAllowedConcurrentCalls
    ) {}

    public record CacheMetrics(
            long hits,
            long misses,
            long size
    ) {}

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private CircuitBreakerMetrics cb;
        private RetryMetrics retry;
        private RateLimiterMetrics rl;
        private BulkheadMetrics bh;
        private CacheMetrics cache;

        public Builder circuitBreaker(CircuitBreakerMetrics v) { cb = v; return this; }
        public Builder retry(RetryMetrics v)                   { retry = v; return this; }
        public Builder rateLimiter(RateLimiterMetrics v)       { rl = v; return this; }
        public Builder bulkhead(BulkheadMetrics v)             { bh = v; return this; }
        public Builder cache(CacheMetrics v)                   { cache = v; return this; }

        public ResilienceMetricsDto build() {
            return new ResilienceMetricsDto(cb, retry, rl, bh, cache, Instant.now().toString());
        }
    }
}
