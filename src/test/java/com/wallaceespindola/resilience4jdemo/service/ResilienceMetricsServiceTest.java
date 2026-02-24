package com.wallaceespindola.resilience4jdemo.service;

import com.wallaceespindola.resilience4jdemo.config.CacheConfig;
import com.wallaceespindola.resilience4jdemo.dto.ResilienceMetricsDto;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ResilienceMetricsService Tests")
class ResilienceMetricsServiceTest {

    private ResilienceMetricsService service;
    private CacheConfig              cacheConfig;

    @BeforeEach
    void setUp() {
        CircuitBreakerRegistry cbRegistry =
                CircuitBreakerRegistry.of(CircuitBreakerConfig.ofDefaults());
        RetryRegistry retryRegistry =
                RetryRegistry.of(RetryConfig.ofDefaults());
        RateLimiterRegistry rlRegistry =
                RateLimiterRegistry.of(RateLimiterConfig.custom()
                        .limitForPeriod(10)
                        .limitRefreshPeriod(Duration.ofSeconds(1))
                        .timeoutDuration(Duration.ofMillis(100))
                        .build());
        BulkheadRegistry bhRegistry =
                BulkheadRegistry.of(BulkheadConfig.ofDefaults());

        cacheConfig = new CacheConfig();
        service = new ResilienceMetricsService(cbRegistry, retryRegistry, rlRegistry, bhRegistry, cacheConfig);
    }

    @Test
    @DisplayName("snapshot() returns non-null result with all modules")
    void snapshot_returnsAllModules() {
        ResilienceMetricsDto dto = service.snapshot();

        assertThat(dto).isNotNull();
        assertThat(dto.circuitBreaker()).isNotNull();
        assertThat(dto.retry()).isNotNull();
        assertThat(dto.rateLimiter()).isNotNull();
        assertThat(dto.bulkhead()).isNotNull();
        assertThat(dto.cache()).isNotNull();
        assertThat(dto.timestamp()).isNotBlank();
    }

    @Test
    @DisplayName("Initial CB state is CLOSED")
    void snapshot_initialCbState_isClosed() {
        assertThat(service.snapshot().circuitBreaker().state()).isEqualTo("CLOSED");
    }

    @Test
    @DisplayName("Cache counters reflect CacheConfig atomics")
    void snapshot_cacheCounters_reflectCacheConfig() {
        cacheConfig.cacheHits.set(5);
        cacheConfig.cacheMisses.set(3);

        ResilienceMetricsDto dto = service.snapshot();

        assertThat(dto.cache().hits()).isEqualTo(5);
        assertThat(dto.cache().misses()).isEqualTo(3);
    }

    @Test
    @DisplayName("RateLimiter metrics show positive available permits")
    void snapshot_rateLimiterPermits_positive() {
        assertThat(service.snapshot().rateLimiter().availablePermissions()).isGreaterThan(0);
    }
}
