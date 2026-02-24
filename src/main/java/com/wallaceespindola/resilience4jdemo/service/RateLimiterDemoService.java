package com.wallaceespindola.resilience4jdemo.service;

import com.wallaceespindola.resilience4jdemo.client.SimulatedDownstreamClient;
import com.wallaceespindola.resilience4jdemo.dto.DemoCallResult;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Demonstrates the RateLimiter pattern.
 *
 * <p>Configured to allow only 5 calls per second. Clicking "Spam 20 requests"
 * shows the first 5 succeeding and the rest being rejected immediately.
 */
@Service
@Slf4j
public class RateLimiterDemoService {

    private final RateLimiter              rateLimiter;
    private final SimulatedDownstreamClient client;
    private final CircuitBreakerRegistry   cbRegistry;

    public RateLimiterDemoService(RateLimiterRegistry registry,
                                  SimulatedDownstreamClient client,
                                  CircuitBreakerRegistry cbRegistry) {
        this.rateLimiter = registry.rateLimiter("downstream");
        this.client      = client;
        this.cbRegistry  = cbRegistry;
    }

    /** Makes a single call through the rate limiter. */
    public DemoCallResult call() {
        long start   = System.currentTimeMillis();
        String cbState = cbRegistry.circuitBreaker("downstream").getState().name();

        Supplier<DemoCallResult> decorated = RateLimiter.decorateSupplier(rateLimiter, () -> {
            client.fetchPage(0, 1);
            long elapsed = System.currentTimeMillis() - start;
            return DemoCallResult.success("RateLimiter",
                    "Permitted (available=%d)".formatted(rateLimiter.getMetrics().getAvailablePermissions()),
                    1, elapsed, cbState);
        });

        try {
            return decorated.get();
        } catch (RequestNotPermitted e) {
            long elapsed = System.currentTimeMillis() - start;
            log.warn("Rate limited: {}", e.getMessage());
            return DemoCallResult.rejected("RateLimiter",
                    "Rate limit exceeded (limit=%d/s)".formatted(
                            rateLimiter.getRateLimiterConfig().getLimitForPeriod()),
                    elapsed, cbState);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            return DemoCallResult.fallback("RateLimiter", e.getMessage(), 1, elapsed, cbState);
        }
    }

    /**
     * Fires {@code n} rapid calls and returns a result per call.
     * Use n=20 to demonstrate: ~5 succeed, ~15 are rate-limited.
     */
    public List<DemoCallResult> spam(int n) {
        List<DemoCallResult> results = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            results.add(call());
        }
        log.info("Spam {} calls — succeeded={}, rejected={}",
                n,
                results.stream().filter(r -> "success".equals(r.outcome())).count(),
                results.stream().filter(r -> "rejected".equals(r.outcome())).count());
        return results;
    }

    public RateLimiter.Metrics getMetrics() {
        return rateLimiter.getMetrics();
    }
}
