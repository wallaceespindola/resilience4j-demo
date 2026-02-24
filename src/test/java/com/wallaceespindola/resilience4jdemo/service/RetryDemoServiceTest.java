package com.wallaceespindola.resilience4jdemo.service;

import com.wallaceespindola.resilience4jdemo.client.SimulatedDownstreamClient;
import com.wallaceespindola.resilience4jdemo.client.SimulatedServerException;
import com.wallaceespindola.resilience4jdemo.dto.DemoCallResult;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
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
@DisplayName("RetryDemoService Tests")
class RetryDemoServiceTest {

    @Mock private SimulatedDownstreamClient client;

    private RetryDemoService     service;
    private CircuitBreakerRegistry cbRegistry;

    @BeforeEach
    void setUp() {
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(10))   // fast for tests
                .retryExceptions(SimulatedServerException.class, RuntimeException.class)
                .build();

        cbRegistry = CircuitBreakerRegistry.of(CircuitBreakerConfig.ofDefaults());
        service = new RetryDemoService(RetryRegistry.of(retryConfig), client, cbRegistry);
    }

    @Test
    @DisplayName("Call succeeds on first attempt when healthy")
    void call_succeeds_firstAttempt() {
        when(client.fetchPage(anyInt(), anyInt())).thenReturn(List.of());

        DemoCallResult result = service.call();

        assertThat(result.outcome()).isEqualTo("success");
        assertThat(result.module()).isEqualTo("Retry");
        // Only 1 attempt was needed
        assertThat(result.attemptNumber()).isEqualTo(1);
    }

    @Test
    @DisplayName("Retries on transient failure then succeeds")
    void call_retriesOnTransientFailure_thenSucceeds() {
        // Fail on first call, succeed on second
        when(client.fetchPage(anyInt(), anyInt()))
                .thenThrow(new SimulatedServerException("Transient"))
                .thenReturn(List.of());

        DemoCallResult result = service.call();

        assertThat(result.outcome()).isEqualTo("success");
        // fetchPage called twice (1 fail + 1 success)
        verify(client, times(2)).fetchPage(anyInt(), anyInt());
    }

    @Test
    @DisplayName("Returns fallback after all attempts exhausted")
    void call_fallback_afterAllAttemptsExhausted() {
        when(client.fetchPage(anyInt(), anyInt()))
                .thenThrow(new SimulatedServerException("Permanent failure"));

        DemoCallResult result = service.call();

        assertThat(result.outcome()).isEqualTo("fallback");
        assertThat(result.detail()).contains("attempt");
        // fetchPage called 3 times (maxAttempts=3)
        verify(client, times(3)).fetchPage(anyInt(), anyInt());
    }

    @Test
    @DisplayName("Response always has non-null timestamp")
    void call_responseAlwaysHasTimestamp() {
        when(client.fetchPage(anyInt(), anyInt())).thenReturn(List.of());

        DemoCallResult result = service.call();

        assertThat(result.timestamp()).isNotBlank();
        assertThat(result.elapsedMs()).isGreaterThanOrEqualTo(0);
    }
}
