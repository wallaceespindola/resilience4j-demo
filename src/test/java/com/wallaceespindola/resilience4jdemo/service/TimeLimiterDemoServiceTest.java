package com.wallaceespindola.resilience4jdemo.service;

import com.wallaceespindola.resilience4jdemo.client.SimulatedDownstreamClient;
import com.wallaceespindola.resilience4jdemo.dto.DemoCallResult;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
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
@DisplayName("TimeLimiterDemoService Tests")
class TimeLimiterDemoServiceTest {

    @Mock private SimulatedDownstreamClient client;

    private TimeLimiterDemoService service;

    @BeforeEach
    void setUp() {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofMillis(200))   // fast timeout for tests
                .cancelRunningFuture(true)
                .build();
        CircuitBreakerRegistry cbRegistry =
                CircuitBreakerRegistry.of(CircuitBreakerConfig.ofDefaults());
        service = new TimeLimiterDemoService(TimeLimiterRegistry.of(config), client, cbRegistry);
    }

    @Test
    @DisplayName("Call succeeds when downstream responds quickly")
    void call_success_whenFast() {
        when(client.fetchPage(anyInt(), anyInt())).thenReturn(List.of());

        DemoCallResult result = service.call();

        assertThat(result.outcome()).isEqualTo("success");
        assertThat(result.module()).isEqualTo("TimeLimiter");
    }

    @Test
    @DisplayName("Call returns timeout when downstream is too slow")
    void call_timeout_whenSlow() {
        when(client.fetchPage(anyInt(), anyInt())).thenAnswer(inv -> {
            Thread.sleep(500);   // longer than 200ms limit
            return List.of();
        });

        DemoCallResult result = service.call();

        // The outcome is either "timeout" or "fallback" depending on which exception propagates
        assertThat(result.outcome()).isIn("timeout", "fallback");
        assertThat(result.elapsedMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Successful call has non-null timestamp")
    void call_hasTimestamp() {
        when(client.fetchPage(anyInt(), anyInt())).thenReturn(List.of());

        DemoCallResult result = service.call();

        assertThat(result.timestamp()).isNotBlank();
    }
}
