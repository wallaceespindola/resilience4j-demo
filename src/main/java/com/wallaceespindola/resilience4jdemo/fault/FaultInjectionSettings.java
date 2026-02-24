package com.wallaceespindola.resilience4jdemo.fault;

import com.wallaceespindola.resilience4jdemo.dto.FaultSettingsDto;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Singleton bean holding the current fault-injection configuration.
 *
 * <p>All downstream client calls read this to decide whether to introduce
 * delays, errors, or other failure modes. Volatile fields ensure
 * visibility across threads without requiring full synchronization.
 */
@Component
@Getter
@Setter
@Slf4j
public class FaultInjectionSettings {

    /** 0–100: percentage of calls that will throw a simulated server error. */
    private volatile int errorRate = 0;

    /** Fixed delay in milliseconds added to every downstream call. */
    private volatile long fixedDelayMs = 0;

    /** Maximum random additional delay in milliseconds (uniform distribution). */
    private volatile long randomDelayMaxMs = 0;

    /**
     * When true the downstream call sleeps 10 s, exceeding the TimeLimiter threshold.
     * Combined with forceHttp500=false so the circuit breaker records a timeout, not a 500.
     */
    private volatile boolean forceTimeout = false;

    /** When true every call throws a 500-equivalent exception immediately. */
    private volatile boolean forceHttp500 = false;

    /**
     * When true downstream simulates a very low throughput cap, causing
     * RateLimiter to reject requests quickly.
     */
    private volatile boolean rateLimitMode = false;

    /** Soft cap on parallel downstream calls used in the Bulkhead demo. */
    private volatile int maxConcurrentDownstream = 10;

    /**
     * Chaos mode enables multiple failure modes simultaneously:
     * 30% error rate + 500 ms base delay + up to 1500 ms extra delay.
     */
    private volatile boolean chaosMode = false;

    // ---- Counters (for metrics / UI display) ----
    private final AtomicInteger totalCallsAttempted = new AtomicInteger(0);
    private final AtomicInteger totalCallsFailed    = new AtomicInteger(0);

    // ---- Named presets ----

    public void applyFlaky()   { errorRate = 50; log.info("Fault: FLAKY (50% error rate)"); }
    public void applySlow()    { fixedDelayMs = 2000; log.info("Fault: SLOW (2000 ms delay)"); }
    public void applyTimeout() { forceTimeout = true; fixedDelayMs = 3000;
                                 log.info("Fault: TIMEOUT (3 s delay > 1.5 s limit)"); }
    public void applyHttp500() { forceHttp500 = true; log.info("Fault: HTTP 500"); }
    public void applyChaos()   { errorRate = 30; fixedDelayMs = 500; randomDelayMaxMs = 1500;
                                 chaosMode = true; log.info("Fault: CHAOS MODE"); }

    public void reset() {
        errorRate = 0; fixedDelayMs = 0; randomDelayMaxMs = 0;
        forceTimeout = false; forceHttp500 = false;
        rateLimitMode = false; maxConcurrentDownstream = 10; chaosMode = false;
        totalCallsAttempted.set(0); totalCallsFailed.set(0);
        log.info("Fault: RESET (all healthy)");
    }

    public FaultSettingsDto toDto() {
        return new FaultSettingsDto(errorRate, fixedDelayMs, randomDelayMaxMs,
                forceTimeout, forceHttp500, rateLimitMode, maxConcurrentDownstream, chaosMode);
    }

    public void applyFrom(FaultSettingsDto dto) {
        errorRate = dto.errorRate();
        fixedDelayMs = dto.fixedDelayMs();
        randomDelayMaxMs = dto.randomDelayMaxMs();
        forceTimeout = dto.forceTimeout();
        forceHttp500 = dto.forceHttp500();
        rateLimitMode = dto.rateLimitMode();
        maxConcurrentDownstream = dto.maxConcurrentDownstream();
        chaosMode = dto.chaosMode();
    }
}
