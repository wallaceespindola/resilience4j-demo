package com.wallaceespindola.resilience4jdemo.service;

import com.wallaceespindola.resilience4jdemo.client.SimulatedDownstreamClient;
import com.wallaceespindola.resilience4jdemo.dto.DemoCallResult;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BulkheadDemoService Tests")
class BulkheadDemoServiceTest {

    @Mock private SimulatedDownstreamClient client;

    private BulkheadDemoService service;

    @BeforeEach
    void setUp() {
        BulkheadConfig bhConfig = BulkheadConfig.custom()
                .maxConcurrentCalls(2)           // very low for testing rejections
                .maxWaitDuration(Duration.ofMillis(0))  // reject immediately if full
                .build();
        BulkheadRegistry bhRegistry = BulkheadRegistry.of(bhConfig);
        CircuitBreakerRegistry cbRegistry =
                CircuitBreakerRegistry.of(CircuitBreakerConfig.ofDefaults());

        service = new BulkheadDemoService(bhRegistry, client, cbRegistry);
    }

    @Test
    @DisplayName("Single call succeeds when bulkhead has capacity")
    void call_success_whenCapacityAvailable() {
        when(client.fetchPage(anyInt(), anyInt())).thenReturn(List.of());

        DemoCallResult result = service.call();

        assertThat(result.outcome()).isEqualTo("success");
        assertThat(result.module()).isEqualTo("Bulkhead");
    }

    @Test
    @DisplayName("Call fails fast when bulkhead is full")
    void call_rejected_whenBulkheadFull() throws InterruptedException {
        // Hold threads inside the bulkhead using a slow response
        when(client.fetchPage(anyInt(), anyInt())).thenAnswer(inv -> {
            Thread.sleep(200);
            return List.of();
        });

        // Launch 2 concurrent calls to fill the bulkhead
        Thread t1 = new Thread(() -> service.call());
        Thread t2 = new Thread(() -> service.call());
        t1.start();
        t2.start();
        Thread.sleep(50);  // give threads time to enter bulkhead

        // Now a third call should be rejected
        DemoCallResult rejected = service.call();

        t1.join(500);
        t2.join(500);

        assertThat(rejected.outcome()).isEqualTo("rejected");
        assertThat(rejected.detail()).contains("concurrent");
    }

    @Test
    @DisplayName("Concurrent call batch returns mix of success and rejected")
    void concurrent_returnsMixedResults() throws InterruptedException {
        when(client.fetchPage(anyInt(), anyInt())).thenReturn(List.of());

        List<DemoCallResult> results = service.concurrent(4);

        assertThat(results).hasSize(4);
        long successes = results.stream().filter(r -> "success".equals(r.outcome())).count();
        long rejections = results.stream().filter(r -> "rejected".equals(r.outcome())).count();
        // With maxConcurrentCalls=2, at least 2 succeed (sequential calls after semaphore released)
        assertThat(successes + rejections).isEqualTo(4);
    }

    @Test
    @DisplayName("Metrics report max allowed concurrent calls")
    void getMetrics_reportsMaxConcurrentCalls() {
        var metrics = service.getMetrics();
        assertThat(metrics.getMaxAllowedConcurrentCalls()).isEqualTo(2);
    }

    @Test
    @DisplayName("DemoCallResult has non-null timestamp")
    void call_resultHasTimestamp() {
        when(client.fetchPage(anyInt(), anyInt())).thenReturn(List.of());
        DemoCallResult result = service.call();
        assertThat(result.timestamp()).isNotBlank();
    }

    @Test
    @DisplayName("concurrent() with n=1 returns a single result")
    void concurrent_withOne_returnsSingleResult() throws InterruptedException {
        when(client.fetchPage(anyInt(), anyInt())).thenReturn(List.of());
        List<DemoCallResult> results = service.concurrent(1);
        assertThat(results).hasSize(1);
    }
}
