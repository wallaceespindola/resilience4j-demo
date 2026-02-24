package com.wallaceespindola.resilience4jdemo.service;

import com.wallaceespindola.resilience4jdemo.client.SimulatedDownstreamClient;
import com.wallaceespindola.resilience4jdemo.domain.TransferRecord;
import com.wallaceespindola.resilience4jdemo.dto.RecordDto;
import com.wallaceespindola.resilience4jdemo.dto.TransferSummary;
import com.wallaceespindola.resilience4jdemo.repo.TransferRecordRepository;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Orchestrates the bulk-transfer scenario.
 *
 * <p>Each page fetch is wrapped by all six Resilience4J modules:
 * <ol>
 *   <li><b>RateLimiter</b> — limits downstream calls per second</li>
 *   <li><b>Bulkhead</b>    — limits concurrent calls</li>
 *   <li><b>CircuitBreaker</b> — stops all calls when failure rate is too high</li>
 *   <li><b>TimeLimiter</b>   — cancels calls that take too long</li>
 *   <li><b>Retry</b>         — retries individual page fetches on transient errors</li>
 * </ol>
 *
 * <p>If all layers fail, a fallback is recorded with placeholder data so the
 * transfer summary accurately reflects exactly what happened.
 */
@Service
@Slf4j
public class TransferService {

    private final SimulatedDownstreamClient client;
    private final TransferRecordRepository  repository;

    private final CircuitBreaker cb;
    private final Retry          retry;
    private final RateLimiter    rateLimiter;
    private final io.github.resilience4j.bulkhead.Bulkhead bulkhead;
    private final TimeLimiter    timeLimiter;

    // Dedicated thread pool for async TimeLimiter futures
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());

    public TransferService(SimulatedDownstreamClient client,
                           TransferRecordRepository repository,
                           CircuitBreakerRegistry cbRegistry,
                           RetryRegistry retryRegistry,
                           RateLimiterRegistry rateLimiterRegistry,
                           BulkheadRegistry bulkheadRegistry,
                           TimeLimiterRegistry timeLimiterRegistry) {
        this.client      = client;
        this.repository  = repository;
        this.cb          = cbRegistry.circuitBreaker("downstream");
        this.retry       = retryRegistry.retry("downstream");
        this.rateLimiter = rateLimiterRegistry.rateLimiter("downstream");
        this.bulkhead    = bulkheadRegistry.bulkhead("downstream");
        this.timeLimiter = timeLimiterRegistry.timeLimiter("downstream");
    }

    /**
     * Transfers {@code totalRecords} records from the simulated downstream API,
     * protected by all six Resilience4J modules.
     *
     * @param totalRecords total number of records to transfer
     * @param pageSize     records per page fetch
     * @return summary of the transfer operation
     */
    @Transactional
    public TransferSummary transfer(int totalRecords, int pageSize) {
        String batchId  = "BATCH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        long   startMs  = System.currentTimeMillis();
        int    pages    = (int) Math.ceil((double) totalRecords / pageSize);

        AtomicInteger pagesSucceeded    = new AtomicInteger(0);
        AtomicInteger pagesFailed       = new AtomicInteger(0);
        AtomicInteger recordsInserted   = new AtomicInteger(0);
        AtomicInteger retriesTotal      = new AtomicInteger(0);
        AtomicInteger fallbacksUsed     = new AtomicInteger(0);
        AtomicInteger cbRejections      = new AtomicInteger(0);
        AtomicInteger bhRejections      = new AtomicInteger(0);
        AtomicInteger rlRejections      = new AtomicInteger(0);
        AtomicInteger timeoutRejections = new AtomicInteger(0);

        // Attach a retry event listener to count retries for this transfer
        retry.getEventPublisher().onRetry(e -> retriesTotal.incrementAndGet());

        log.info("Transfer started: batchId={}, totalRecords={}, pages={}", batchId, totalRecords, pages);

        for (int page = 0; page < pages; page++) {
            final int currentPage = page;
            try {
                List<RecordDto> records = fetchPageWithAllResilience(currentPage, pageSize);
                persistRecords(records, batchId, "inserted");
                recordsInserted.addAndGet(records.size());
                pagesSucceeded.incrementAndGet();

            } catch (CallNotPermittedException e) {
                log.warn("CB OPEN — page {} rejected: {}", currentPage, e.getMessage());
                cbRejections.incrementAndGet();
                pagesFailed.incrementAndGet();
                fallbacksUsed.incrementAndGet();
                persistFallbackPage(currentPage, pageSize, batchId);

            } catch (BulkheadFullException e) {
                log.warn("Bulkhead FULL — page {} rejected: {}", currentPage, e.getMessage());
                bhRejections.incrementAndGet();
                pagesFailed.incrementAndGet();
                fallbacksUsed.incrementAndGet();
                persistFallbackPage(currentPage, pageSize, batchId);

            } catch (RequestNotPermitted e) {
                log.warn("Rate LIMITED — page {} rejected", currentPage);
                rlRejections.incrementAndGet();
                pagesFailed.incrementAndGet();
                fallbacksUsed.incrementAndGet();
                persistFallbackPage(currentPage, pageSize, batchId);

            } catch (TimeoutException e) {
                log.warn("TIMEOUT — page {} timed out", currentPage);
                timeoutRejections.incrementAndGet();
                pagesFailed.incrementAndGet();
                fallbacksUsed.incrementAndGet();
                persistFallbackPage(currentPage, pageSize, batchId);

            } catch (Exception e) {
                log.warn("FAILED — page {}: {}", currentPage, e.getMessage());
                pagesFailed.incrementAndGet();
                fallbacksUsed.incrementAndGet();
                persistFallbackPage(currentPage, pageSize, batchId);
            }
        }

        long durationMs = System.currentTimeMillis() - startMs;
        log.info("Transfer complete: batchId={}, succeeded={}/{}, duration={}ms",
                batchId, pagesSucceeded.get(), pages, durationMs);

        return TransferSummary.builder()
                .batchId(batchId)
                .totalRequested(totalRecords)
                .pagesAttempted(pages)
                .pagesSucceeded(pagesSucceeded.get())
                .pagesFailed(pagesFailed.get())
                .recordsInserted(recordsInserted.get())
                .retriesTotal(retriesTotal.get())
                .fallbacksUsed(fallbacksUsed.get())
                .circuitBreakerRejections(cbRejections.get())
                .bulkheadRejections(bhRejections.get())
                .rateLimiterRejections(rlRejections.get())
                .timeoutRejections(timeoutRejections.get())
                .durationMs(durationMs)
                .build();
    }

    /**
     * Fetches a single page wrapped by RateLimiter → Bulkhead → CircuitBreaker → TimeLimiter → Retry.
     *
     * <p>Each layer is applied programmatically so learners can inspect exactly
     * which decorator kicks in and why.
     */
    private List<RecordDto> fetchPageWithAllResilience(int page, int pageSize)
            throws Exception {

        // Innermost: the actual call, wrapped with TimeLimiter (async)
        Supplier<CompletableFuture<List<RecordDto>>> futureSupplier =
                () -> CompletableFuture.supplyAsync(() -> client.fetchPage(page, pageSize));

        // TimeLimiter wraps the async future
        Callable<List<RecordDto>> timedCall = timeLimiter.decorateFutureSupplier(futureSupplier);

        // Retry wraps the timed call
        Callable<List<RecordDto>> retriedCall = Retry.decorateCallable(retry, timedCall);

        // CircuitBreaker wraps retry
        Callable<List<RecordDto>> cbCall = CircuitBreaker.decorateCallable(cb, retriedCall);

        // Bulkhead wraps circuit breaker
        Callable<List<RecordDto>> bhCall =
                io.github.resilience4j.bulkhead.Bulkhead.decorateCallable(bulkhead, cbCall);

        // RateLimiter is the outermost guard
        Callable<List<RecordDto>> rlCall = RateLimiter.decorateCallable(rateLimiter, bhCall);

        return rlCall.call();
    }

    private void persistRecords(List<RecordDto> records, String batchId, String status) {
        List<TransferRecord> entities = records.stream()
                .map(r -> TransferRecord.builder()
                        .batchId(batchId)
                        .externalId(r.externalId())
                        .name(r.name())
                        .category(r.category())
                        .value(r.value())
                        .sourcePage(r.page())
                        .status(status)
                        .transferredAt(Instant.now())
                        .build())
                .toList();
        repository.saveAll(entities);
    }

    private void persistFallbackPage(int page, int pageSize, String batchId) {
        // Persist placeholder records so every requested record has a DB entry
        List<RecordDto> placeholders = Collections.nCopies(pageSize,
                new RecordDto("FALLBACK-P%d".formatted(page), "FALLBACK", "NONE", "0.00", page, 0));
        persistRecords(placeholders, batchId, "fallback");
    }

    /** Returns all batch IDs with record counts for the history endpoint. */
    public List<String> listBatchIds() {
        return repository.findDistinctBatchIds();
    }

    /** Returns transfer records for a given batchId. */
    public List<TransferRecord> getRecordsForBatch(String batchId) {
        return repository.findByBatchId(batchId);
    }
}
