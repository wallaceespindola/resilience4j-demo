package com.wallaceespindola.resilience4jdemo.service;

import com.wallaceespindola.resilience4jdemo.config.CacheConfig;
import com.wallaceespindola.resilience4jdemo.dto.ResilienceMetricsDto;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.stereotype.Service;

/** Aggregates current metrics from all six R4J modules for the dashboard. */
@Service
public class ResilienceMetricsService {

    private final CircuitBreaker cb;
    private final Retry          retry;
    private final RateLimiter    rateLimiter;
    private final Bulkhead       bulkhead;
    private final CacheConfig    cacheConfig;

    public ResilienceMetricsService(CircuitBreakerRegistry cbRegistry,
                                    RetryRegistry retryRegistry,
                                    RateLimiterRegistry rlRegistry,
                                    BulkheadRegistry bhRegistry,
                                    CacheConfig cacheConfig) {
        this.cb          = cbRegistry.circuitBreaker("downstream");
        this.retry       = retryRegistry.retry("downstream");
        this.rateLimiter = rlRegistry.rateLimiter("downstream");
        this.bulkhead    = bhRegistry.bulkhead("downstream");
        this.cacheConfig = cacheConfig;
    }

    public ResilienceMetricsDto snapshot() {
        CircuitBreaker.Metrics cbm = cb.getMetrics();
        Retry.Metrics rm           = retry.getMetrics();
        RateLimiter.Metrics rlm    = rateLimiter.getMetrics();
        Bulkhead.Metrics bhm       = bulkhead.getMetrics();

        return ResilienceMetricsDto.builder()
                .circuitBreaker(new ResilienceMetricsDto.CircuitBreakerMetrics(
                        cb.getState().name(),
                        cbm.getFailureRate(),
                        cbm.getSlowCallRate(),
                        cbm.getNumberOfNotPermittedCalls(),
                        cbm.getNumberOfSuccessfulCalls(),
                        cbm.getNumberOfFailedCalls()))
                .retry(new ResilienceMetricsDto.RetryMetrics(
                        rm.getNumberOfSuccessfulCallsWithRetryAttempt(),
                        rm.getNumberOfSuccessfulCallsWithoutRetryAttempt(),
                        rm.getNumberOfFailedCallsWithRetryAttempt(),
                        rm.getNumberOfFailedCallsWithoutRetryAttempt()))
                .rateLimiter(new ResilienceMetricsDto.RateLimiterMetrics(
                        rlm.getAvailablePermissions(),
                        rlm.getNumberOfWaitingThreads()))
                .bulkhead(new ResilienceMetricsDto.BulkheadMetrics(
                        bhm.getAvailableConcurrentCalls(),
                        bhm.getMaxAllowedConcurrentCalls()))
                .cache(new ResilienceMetricsDto.CacheMetrics(
                        cacheConfig.cacheHits.get(),
                        cacheConfig.cacheMisses.get(),
                        cacheConfig.cacheHits.get() + cacheConfig.cacheMisses.get()))
                .build();
    }
}
