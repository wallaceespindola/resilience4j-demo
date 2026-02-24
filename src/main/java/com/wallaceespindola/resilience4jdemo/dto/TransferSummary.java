package com.wallaceespindola.resilience4jdemo.dto;

import java.time.Instant;

/** Summary returned after a bulk-transfer operation. */
public record TransferSummary(
        String batchId,
        int totalRequested,
        int pagesAttempted,
        int pagesSucceeded,
        int pagesFailed,
        int recordsInserted,
        int retriesTotal,
        int fallbacksUsed,
        int circuitBreakerRejections,
        int bulkheadRejections,
        int rateLimiterRejections,
        int timeoutRejections,
        long durationMs,
        String timestamp
) {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String batchId;
        private int totalRequested, pagesAttempted, pagesSucceeded, pagesFailed;
        private int recordsInserted, retriesTotal, fallbacksUsed;
        private int circuitBreakerRejections, bulkheadRejections, rateLimiterRejections, timeoutRejections;
        private long durationMs;

        public Builder batchId(String v)                    { batchId = v; return this; }
        public Builder totalRequested(int v)                { totalRequested = v; return this; }
        public Builder pagesAttempted(int v)                { pagesAttempted = v; return this; }
        public Builder pagesSucceeded(int v)                { pagesSucceeded = v; return this; }
        public Builder pagesFailed(int v)                   { pagesFailed = v; return this; }
        public Builder recordsInserted(int v)               { recordsInserted = v; return this; }
        public Builder retriesTotal(int v)                  { retriesTotal = v; return this; }
        public Builder fallbacksUsed(int v)                 { fallbacksUsed = v; return this; }
        public Builder circuitBreakerRejections(int v)      { circuitBreakerRejections = v; return this; }
        public Builder bulkheadRejections(int v)            { bulkheadRejections = v; return this; }
        public Builder rateLimiterRejections(int v)         { rateLimiterRejections = v; return this; }
        public Builder timeoutRejections(int v)             { timeoutRejections = v; return this; }
        public Builder durationMs(long v)                   { durationMs = v; return this; }

        public TransferSummary build() {
            return new TransferSummary(batchId, totalRequested, pagesAttempted, pagesSucceeded,
                    pagesFailed, recordsInserted, retriesTotal, fallbacksUsed,
                    circuitBreakerRejections, bulkheadRejections, rateLimiterRejections,
                    timeoutRejections, durationMs, Instant.now().toString());
        }
    }
}
