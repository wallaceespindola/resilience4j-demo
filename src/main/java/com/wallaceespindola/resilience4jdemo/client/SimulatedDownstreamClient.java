package com.wallaceespindola.resilience4jdemo.client;

import com.wallaceespindola.resilience4jdemo.dto.RecordDto;
import com.wallaceespindola.resilience4jdemo.fault.FaultInjectionSettings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simulates an external source REST API that can be configured to behave poorly.
 *
 * <p>All calls read the current {@link FaultInjectionSettings} and apply:
 * <ul>
 *   <li>Fixed and random delays (slow response / timeout scenario)</li>
 *   <li>Random error rate (flaky scenario)</li>
 *   <li>Forced HTTP 500 (hard-down scenario)</li>
 * </ul>
 *
 * No real network calls are made; data is generated in-memory.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SimulatedDownstreamClient {

    private final FaultInjectionSettings settings;
    private final Random random = new Random();

    // Monotonically increasing call counter for unique IDs
    private final AtomicInteger callCounter = new AtomicInteger(0);

    // Simple in-memory metadata cache baseline (pre-fault)
    private static final Map<String, String> METADATA = Map.of(
            "region",    "EU-WEST-1",
            "env",       "demo",
            "version",   "2.0",
            "owner",     "platform-team",
            "sla",       "99.9%"
    );

    /**
     * Fetches a page of records from the simulated downstream API.
     * Applies all active fault-injection settings before returning data.
     *
     * @param page     zero-based page index
     * @param pageSize number of records per page
     * @return list of generated records
     * @throws SimulatedServerException on injected server errors
     */
    public List<RecordDto> fetchPage(int page, int pageSize) {
        int seq = callCounter.incrementAndGet();
        settings.getTotalCallsAttempted().incrementAndGet();
        log.debug("fetchPage called: page={}, pageSize={}, seq={}", page, pageSize, seq);

        applyFaults("fetchPage#" + seq);

        return generateRecords(page, pageSize);
    }

    /**
     * Fetches metadata for a given key. Used by the Cache demo.
     * Applies fault injection to show the value of caching unreliable lookups.
     *
     * @param key metadata key
     * @return metadata value
     */
    public String fetchMetadata(String key) {
        settings.getTotalCallsAttempted().incrementAndGet();
        log.debug("fetchMetadata: key={}", key);

        applyFaults("fetchMetadata/" + key);

        return METADATA.getOrDefault(key, "unknown-" + key);
    }

    // ---- Private helpers ----

    private void applyFaults(String context) {
        // 1. Force HTTP 500 (hard failure)
        if (settings.isForceHttp500()) {
            settings.getTotalCallsFailed().incrementAndGet();
            throw new SimulatedServerException("Forced HTTP 500 [" + context + "]");
        }

        // 2. Fixed delay
        if (settings.getFixedDelayMs() > 0) {
            sleep(settings.getFixedDelayMs());
        }

        // 3. Random additional delay
        if (settings.getRandomDelayMaxMs() > 0) {
            long extra = (long) (random.nextDouble() * settings.getRandomDelayMaxMs());
            if (extra > 0) sleep(extra);
        }

        // 4. Force timeout (sleep longer than any TimeLimiter threshold)
        if (settings.isForceTimeout()) {
            sleep(10_000);
        }

        // 5. Random error rate
        if (settings.getErrorRate() > 0 && random.nextInt(100) < settings.getErrorRate()) {
            settings.getTotalCallsFailed().incrementAndGet();
            throw new SimulatedServerException(
                    "Random failure [" + context + "] (errorRate=" + settings.getErrorRate() + "%)");
        }
    }

    private List<RecordDto> generateRecords(int page, int pageSize) {
        List<RecordDto> records = new ArrayList<>(pageSize);
        String[] categories = {"PAYMENT", "TRANSFER", "DEPOSIT", "WITHDRAWAL", "FEE"};
        for (int i = 0; i < pageSize; i++) {
            records.add(new RecordDto(
                    "EXT-%04d-%04d".formatted(page, i),
                    "Record-P%d-I%d".formatted(page, i),
                    categories[(page + i) % categories.length],
                    "%.2f".formatted(random.nextDouble() * 10_000),
                    page,
                    i
            ));
        }
        return records;
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Sleep interrupted", e);
        }
    }
}
