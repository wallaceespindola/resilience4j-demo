package com.wallaceespindola.resilience4jdemo.service;

import com.wallaceespindola.resilience4jdemo.client.SimulatedDownstreamClient;
import com.wallaceespindola.resilience4jdemo.dto.DemoCallResult;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Demonstrates the Bulkhead (semaphore) pattern.
 *
 * <p>The bulkhead allows at most 5 concurrent calls (configured in application.properties).
 * Triggering 10 concurrent calls shows 5 succeeding and 5 being rejected immediately.
 */
@Service
@Slf4j
public class BulkheadDemoService {

    private final Bulkhead                 bulkhead;
    private final SimulatedDownstreamClient client;
    private final CircuitBreakerRegistry   cbRegistry;

    public BulkheadDemoService(BulkheadRegistry registry,
                               SimulatedDownstreamClient client,
                               CircuitBreakerRegistry cbRegistry) {
        this.bulkhead   = registry.bulkhead("downstream");
        this.client     = client;
        this.cbRegistry = cbRegistry;
    }

    /** Makes a single call through the bulkhead. */
    public DemoCallResult call() {
        long start   = System.currentTimeMillis();
        String cbState = cbRegistry.circuitBreaker("downstream").getState().name();

        Supplier<DemoCallResult> decorated = Bulkhead.decorateSupplier(bulkhead, () -> {
            client.fetchPage(0, 1);
            long elapsed = System.currentTimeMillis() - start;
            return DemoCallResult.success("Bulkhead",
                    "Admitted (available=%d)".formatted(bulkhead.getMetrics().getAvailableConcurrentCalls()),
                    1, elapsed, cbState);
        });

        try {
            return decorated.get();
        } catch (BulkheadFullException e) {
            long elapsed = System.currentTimeMillis() - start;
            log.warn("Bulkhead full");
            return DemoCallResult.rejected("Bulkhead",
                    "Too many concurrent calls (max=%d)".formatted(
                            bulkhead.getBulkheadConfig().getMaxConcurrentCalls()),
                    elapsed, cbState);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            return DemoCallResult.fallback("Bulkhead", e.getMessage(), 1, elapsed, cbState);
        }
    }

    /**
     * Fires {@code n} calls concurrently to trigger bulkhead rejections.
     * With max=5 and n=10, approximately 5 will be admitted and 5 rejected.
     */
    public List<DemoCallResult> concurrent(int n) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(n);
        List<Future<DemoCallResult>> futures = new ArrayList<>(n);

        for (int i = 0; i < n; i++) {
            futures.add(pool.submit(this::call));
        }

        List<DemoCallResult> results = new ArrayList<>(n);
        for (Future<DemoCallResult> f : futures) {
            try {
                results.add(f.get(5, TimeUnit.SECONDS));
            } catch (ExecutionException | TimeoutException e) {
                results.add(DemoCallResult.rejected("Bulkhead", e.getMessage(), 0, "UNKNOWN"));
            }
        }

        pool.shutdown();
        log.info("Concurrent {} calls — admitted={}, rejected={}",
                n,
                results.stream().filter(r -> "success".equals(r.outcome())).count(),
                results.stream().filter(r -> "rejected".equals(r.outcome())).count());
        return results;
    }

    public Bulkhead.Metrics getMetrics() {
        return bulkhead.getMetrics();
    }
}
