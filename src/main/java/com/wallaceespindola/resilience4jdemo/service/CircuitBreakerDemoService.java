package com.wallaceespindola.resilience4jdemo.service;

import com.wallaceespindola.resilience4jdemo.client.SimulatedDownstreamClient;
import com.wallaceespindola.resilience4jdemo.dto.DemoCallResult;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

/**
 * Demonstrates the CircuitBreaker pattern in isolation.
 *
 * <p>The circuit trips to OPEN when more than 50% of the last 10 calls fail
 * (configured in application.properties). After 10 seconds it transitions to
 * HALF_OPEN and allows 3 probe calls to verify recovery.
 */
@Service
@Slf4j
public class CircuitBreakerDemoService {

    private final CircuitBreaker           cb;
    private final SimulatedDownstreamClient client;

    public CircuitBreakerDemoService(CircuitBreakerRegistry registry,
                                     SimulatedDownstreamClient client) {
        this.cb     = registry.circuitBreaker("downstream");
        this.client = client;
    }

    /** Makes a single call through the circuit breaker. Falls back on rejection. */
    public DemoCallResult call() {
        long start = System.currentTimeMillis();
        Supplier<DemoCallResult> decorated = CircuitBreaker.decorateSupplier(cb, () -> {
            client.fetchPage(0, 1);   // single record fetch as proof-of-call
            return DemoCallResult.success("CircuitBreaker",
                    "Call succeeded — CB state: " + cb.getState(),
                    1, System.currentTimeMillis() - start, cb.getState().name());
        });

        try {
            return decorated.get();
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            String msg = e.getClass().getSimpleName() + ": " + e.getMessage();
            log.debug("CB call failed: {}", msg);
            return DemoCallResult.fallback("CircuitBreaker", msg, 1, elapsed, cb.getState().name());
        }
    }

    /** Returns the current circuit-breaker state and metrics. */
    public String getState() {
        return cb.getState().name();
    }

    public CircuitBreaker.Metrics getMetrics() {
        return cb.getMetrics();
    }

    /** Transitions the CB to FORCED_OPEN (manual open for demo). */
    public void forceOpen() {
        cb.transitionToForcedOpenState();
        log.info("CB manually FORCED OPEN");
    }

    /** Resets the CB to CLOSED. */
    public void reset() {
        cb.transitionToClosedState();
        log.info("CB manually reset to CLOSED");
    }
}
