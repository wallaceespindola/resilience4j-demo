package com.wallaceespindola.resilience4jdemo.service;

import com.wallaceespindola.resilience4jdemo.client.SimulatedDownstreamClient;
import com.wallaceespindola.resilience4jdemo.dto.DemoCallResult;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimiterDemoService Tests")
class RateLimiterDemoServiceTest {

    @Mock private SimulatedDownstreamClient client;

    private RateLimiterDemoService service;

    @BeforeEach
    void setUp() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(3)
                .limitRefreshPeriod(Duration.ofSeconds(60))  // long window so it doesn't refill in tests
                .timeoutDuration(Duration.ofMillis(0))       // reject immediately if no permit
                .build();
        CircuitBreakerRegistry cbRegistry =
                CircuitBreakerRegistry.of(CircuitBreakerConfig.ofDefaults());
        service = new RateLimiterDemoService(RateLimiterRegistry.of(config), client, cbRegistry);
    }

    @Test
    @DisplayName("First call within limit succeeds")
    void call_success_withinLimit() {
        when(client.fetchPage(anyInt(), anyInt())).thenReturn(List.of());

        DemoCallResult result = service.call();

        assertThat(result.outcome()).isEqualTo("success");
        assertThat(result.module()).isEqualTo("RateLimiter");
    }

    @Test
    @DisplayName("Call is rejected after limit exhausted")
    void call_rejected_afterLimitExhausted() {
        when(client.fetchPage(anyInt(), anyInt())).thenReturn(List.of());

        // Exhaust all 3 permits
        service.call(); service.call(); service.call();

        // 4th call should be rejected
        DemoCallResult rejected = service.call();
        assertThat(rejected.outcome()).isEqualTo("rejected");
        assertThat(rejected.detail()).contains("Rate limit exceeded");
    }

    @Test
    @DisplayName("spam() returns the requested number of results")
    void spam_returnsRequestedCount() {
        when(client.fetchPage(anyInt(), anyInt())).thenReturn(List.of());

        List<DemoCallResult> results = service.spam(5);

        assertThat(results).hasSize(5);
    }

    @Test
    @DisplayName("spam() mix: first N succeed, rest are rejected")
    void spam_firstNSucceedRestRejected() {
        when(client.fetchPage(anyInt(), anyInt())).thenReturn(List.of());

        List<DemoCallResult> results = service.spam(5); // limit=3

        long successes  = results.stream().filter(r -> "success".equals(r.outcome())).count();
        long rejections = results.stream().filter(r -> "rejected".equals(r.outcome())).count();

        assertThat(successes).isEqualTo(3);
        assertThat(rejections).isEqualTo(2);
    }

    @Test
    @DisplayName("getMetrics returns non-null metrics")
    void getMetrics_notNull() {
        assertThat(service.getMetrics()).isNotNull();
    }

    @Test
    @DisplayName("DemoCallResult has non-null timestamp")
    void call_hasTimestamp() {
        when(client.fetchPage(anyInt(), anyInt())).thenReturn(List.of());
        assertThat(service.call().timestamp()).isNotBlank();
    }
}
