package com.wallaceespindola.resilience4jdemo.service;

import com.wallaceespindola.resilience4jdemo.client.SimulatedDownstreamClient;
import com.wallaceespindola.resilience4jdemo.client.SimulatedServerException;
import com.wallaceespindola.resilience4jdemo.dto.DemoCallResult;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CircuitBreakerDemoService Tests")
class CircuitBreakerDemoServiceTest {

    @Mock private SimulatedDownstreamClient client;

    private CircuitBreakerDemoService service;
    private CircuitBreaker             cb;

    @BeforeEach
    void setUp() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(5)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(60)       // trip after 60% failures
                .waitDurationInOpenState(Duration.ofMillis(100))
                .permittedNumberOfCallsInHalfOpenState(2)
                .build();
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        cb = registry.circuitBreaker("downstream");
        service = new CircuitBreakerDemoService(registry, client);
    }

    @Test
    @DisplayName("Call succeeds when downstream is healthy")
    void call_success_whenHealthy() {
        when(client.fetchPage(anyInt(), anyInt())).thenReturn(List.of());

        DemoCallResult result = service.call();

        assertThat(result.outcome()).isEqualTo("success");
        assertThat(result.module()).isEqualTo("CircuitBreaker");
        assertThat(result.timestamp()).isNotBlank();
    }

    @Test
    @DisplayName("Call returns fallback when downstream throws")
    void call_fallback_whenDownstreamFails() {
        when(client.fetchPage(anyInt(), anyInt()))
                .thenThrow(new SimulatedServerException("Downstream error"));

        DemoCallResult result = service.call();

        assertThat(result.outcome()).isEqualTo("fallback");
        assertThat(result.detail()).contains("SimulatedServerException");
    }

    @Test
    @DisplayName("Circuit breaker transitions to OPEN after threshold failures")
    void circuitBreaker_opensAfterFailures() {
        when(client.fetchPage(anyInt(), anyInt()))
                .thenThrow(new SimulatedServerException("Forced error"));

        // Drive 5 failures (minimum window = 5, threshold = 60%)
        for (int i = 0; i < 5; i++) {
            service.call();
        }

        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(service.getState()).isEqualTo("OPEN");
    }

    @Test
    @DisplayName("forceOpen transitions CB to FORCED_OPEN")
    void forceOpen_transitionsToForcedOpen() {
        service.forceOpen();
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.FORCED_OPEN);
    }

    @Test
    @DisplayName("reset transitions CB to CLOSED")
    void reset_transitionsToClosed() {
        service.forceOpen();
        service.reset();
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("getState returns string state name")
    void getState_returnsStateName() {
        assertThat(service.getState()).isEqualTo("CLOSED");
        service.forceOpen();
        assertThat(service.getState()).isEqualTo("FORCED_OPEN");
    }

    @Test
    @DisplayName("Metrics are available and contain expected fields")
    void getMetrics_returnsMetrics() {
        when(client.fetchPage(anyInt(), anyInt())).thenReturn(List.of());
        service.call();

        CircuitBreaker.Metrics metrics = service.getMetrics();
        assertThat(metrics).isNotNull();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isGreaterThanOrEqualTo(0);
    }
}
