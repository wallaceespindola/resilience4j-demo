package com.wallaceespindola.resilience4jdemo.service;

import com.wallaceespindola.resilience4jdemo.client.SimulatedDownstreamClient;
import com.wallaceespindola.resilience4jdemo.dto.DemoCallResult;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Demonstrates the TimeLimiter pattern.
 *
 * <p>The limiter is configured to cancel futures after 1500 ms.
 * Enable "Force Timeout" in fault injection (adds 3000 ms delay) to
 * trigger a TimeoutException and observe the fallback.
 */
@Service
@Slf4j
public class TimeLimiterDemoService {

    private final TimeLimiter              timeLimiter;
    private final SimulatedDownstreamClient client;
    private final CircuitBreakerRegistry   cbRegistry;
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(2);

    public TimeLimiterDemoService(TimeLimiterRegistry registry,
                                  SimulatedDownstreamClient client,
                                  CircuitBreakerRegistry cbRegistry) {
        this.timeLimiter = registry.timeLimiter("downstream");
        this.client      = client;
        this.cbRegistry  = cbRegistry;
    }

    /** Makes an async call wrapped by the TimeLimiter. */
    public DemoCallResult call() {
        long   start   = System.currentTimeMillis();
        String cbState = cbRegistry.circuitBreaker("downstream").getState().name();
        long   limitMs = timeLimiter.getTimeLimiterConfig().getTimeoutDuration().toMillis();

        Supplier<CompletableFuture<String>> futureSupplier = () ->
                CompletableFuture.supplyAsync(() -> {
                    client.fetchPage(0, 1);
                    return "ok";
                });

        try {
            timeLimiter.executeFutureSupplier(futureSupplier);
            long elapsed = System.currentTimeMillis() - start;
            return DemoCallResult.success("TimeLimiter",
                    "Completed within %dms (limit=%dms)".formatted(elapsed, limitMs),
                    1, elapsed, cbState);
        } catch (TimeoutException e) {
            long elapsed = System.currentTimeMillis() - start;
            log.warn("TimeLimiter: timeout after {}ms (limit={}ms)", elapsed, limitMs);
            return DemoCallResult.timeout("TimeLimiter",
                    "Timed out after %dms (limit=%dms) — fallback served".formatted(elapsed, limitMs),
                    elapsed, cbState);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.warn("TimeLimiter call failed: {}", e.getMessage());
            return DemoCallResult.fallback("TimeLimiter", e.getMessage(), 1, elapsed, cbState);
        }
    }
}
