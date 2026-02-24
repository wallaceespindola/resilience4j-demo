package com.wallaceespindola.resilience4jdemo.service;

import com.wallaceespindola.resilience4jdemo.client.SimulatedDownstreamClient;
import com.wallaceespindola.resilience4jdemo.dto.DemoCallResult;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demonstrates the Retry pattern.
 *
 * <p>Configured for up to 3 attempts with exponential backoff (300 ms → 600 ms).
 * Set errorRate=50% via fault injection to see retries in action.
 */
@Service
@Slf4j
public class RetryDemoService {

    private final Retry                    retry;
    private final SimulatedDownstreamClient client;
    private final CircuitBreakerRegistry   cbRegistry;

    public RetryDemoService(RetryRegistry retryRegistry,
                            SimulatedDownstreamClient client,
                            CircuitBreakerRegistry cbRegistry) {
        this.retry      = retryRegistry.retry("downstream");
        this.client     = client;
        this.cbRegistry = cbRegistry;
    }

    /** Makes a call with retry. Returns how many attempts were needed. */
    public DemoCallResult call() {
        long         start    = System.currentTimeMillis();
        AtomicInteger attempts = new AtomicInteger(0);

        // Track attempts via event listener (scoped to this call)
        var listener = retry.getEventPublisher().onRetry(e -> {
            attempts.incrementAndGet();
            log.info("Retry attempt #{} after: {}", e.getNumberOfRetryAttempts(), e.getLastThrowable().getMessage());
        });

        String cbState = cbRegistry.circuitBreaker("downstream").getState().name();
        try {
            Retry.decorateCallable(retry, () -> {
                client.fetchPage(0, 1);
                return null;
            }).call();

            long elapsed = System.currentTimeMillis() - start;
            int  total   = attempts.get() + 1; // attempts = retries, total includes first try
            return DemoCallResult.success("Retry",
                    "Succeeded after %d attempt(s)".formatted(total),
                    total, elapsed, cbState);

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            int  total   = retry.getRetryConfig().getMaxAttempts();
            log.warn("All {} retry attempts exhausted: {}", total, e.getMessage());
            return DemoCallResult.fallback("Retry",
                    "All %d attempts failed: %s".formatted(total, e.getMessage()),
                    total, elapsed, cbState);
        }
    }
}
