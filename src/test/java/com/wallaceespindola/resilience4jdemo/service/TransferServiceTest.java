package com.wallaceespindola.resilience4jdemo.service;

import com.wallaceespindola.resilience4jdemo.client.SimulatedDownstreamClient;
import com.wallaceespindola.resilience4jdemo.client.SimulatedServerException;
import com.wallaceespindola.resilience4jdemo.domain.TransferRecord;
import com.wallaceespindola.resilience4jdemo.dto.RecordDto;
import com.wallaceespindola.resilience4jdemo.dto.TransferSummary;
import com.wallaceespindola.resilience4jdemo.fault.FaultInjectionSettings;
import com.wallaceespindola.resilience4jdemo.repo.TransferRecordRepository;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
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
@DisplayName("TransferService Tests")
class TransferServiceTest {

    @Mock private SimulatedDownstreamClient client;
    @Mock private TransferRecordRepository  repository;

    private TransferService service;

    @BeforeEach
    void setUp() {
        // Use very permissive R4J configs so tests don't get blocked by resilience rules
        CircuitBreakerRegistry cbRegistry = CircuitBreakerRegistry.of(
                CircuitBreakerConfig.custom()
                        .slidingWindowSize(100)
                        .failureRateThreshold(90)
                        .minimumNumberOfCalls(50)
                        .build());

        RetryRegistry retryRegistry = RetryRegistry.of(
                RetryConfig.custom()
                        .maxAttempts(2)
                        .waitDuration(Duration.ofMillis(10))
                        .build());

        RateLimiterRegistry rlRegistry = RateLimiterRegistry.of(
                RateLimiterConfig.custom()
                        .limitForPeriod(1000)
                        .limitRefreshPeriod(Duration.ofMillis(100))
                        .timeoutDuration(Duration.ofMillis(50))
                        .build());

        BulkheadRegistry bhRegistry = BulkheadRegistry.of(
                BulkheadConfig.custom()
                        .maxConcurrentCalls(50)
                        .maxWaitDuration(Duration.ofMillis(50))
                        .build());

        TimeLimiterRegistry tlRegistry = TimeLimiterRegistry.of(
                TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(5))
                        .build());

        service = new TransferService(client, repository, cbRegistry,
                retryRegistry, rlRegistry, bhRegistry, tlRegistry);
    }

    @Test
    @DisplayName("Successful transfer persists all records")
    void successfulTransfer_persistsAllRecords() {
        List<RecordDto> page = List.of(
                new RecordDto("EXT-001", "Name1", "CAT", "100.00", 0, 0),
                new RecordDto("EXT-002", "Name2", "CAT", "200.00", 0, 1)
        );
        when(client.fetchPage(anyInt(), anyInt())).thenReturn(page);
        when(repository.saveAll(any())).thenReturn(List.of());

        TransferSummary summary = service.transfer(4, 2);

        assertThat(summary.pagesSucceeded()).isEqualTo(2);
        assertThat(summary.pagesFailed()).isEqualTo(0);
        assertThat(summary.recordsInserted()).isEqualTo(4);
        assertThat(summary.fallbacksUsed()).isEqualTo(0);
        assertThat(summary.batchId()).startsWith("BATCH-");
        assertThat(summary.timestamp()).isNotBlank();
        verify(repository, times(2)).saveAll(any());
    }

    @Test
    @DisplayName("Failing downstream triggers fallback for each failed page")
    void failingDownstream_usesFallback() {
        when(client.fetchPage(anyInt(), anyInt()))
                .thenThrow(new SimulatedServerException("Forced 500"));
        when(repository.saveAll(any())).thenReturn(List.of());

        TransferSummary summary = service.transfer(10, 5);

        assertThat(summary.pagesFailed()).isEqualTo(2);
        assertThat(summary.pagesSucceeded()).isEqualTo(0);
        assertThat(summary.fallbacksUsed()).isEqualTo(2);
        assertThat(summary.durationMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Mixed results: some pages succeed, some fail")
    void mixedResults_partialTransfer() {
        List<RecordDto> page = List.of(new RecordDto("EXT-001", "N1", "CAT", "1.00", 0, 0));
        when(client.fetchPage(eq(0), anyInt())).thenReturn(page);
        when(client.fetchPage(eq(1), anyInt()))
                .thenThrow(new SimulatedServerException("Flaky"));
        when(repository.saveAll(any())).thenReturn(List.of());

        TransferSummary summary = service.transfer(2, 1);

        assertThat(summary.pagesSucceeded()).isEqualTo(1);
        assertThat(summary.pagesFailed()).isEqualTo(1);
    }

    @Test
    @DisplayName("Single page transfer works correctly")
    void singlePage_transfer() {
        List<RecordDto> page = List.of(new RecordDto("EXT-001", "N1", "CAT", "1.00", 0, 0));
        when(client.fetchPage(anyInt(), anyInt())).thenReturn(page);
        when(repository.saveAll(any())).thenReturn(List.of());

        TransferSummary summary = service.transfer(1, 10);

        assertThat(summary.pagesAttempted()).isEqualTo(1);
        assertThat(summary.recordsInserted()).isEqualTo(1);
    }

    @Test
    @DisplayName("listBatchIds delegates to repository")
    void listBatchIds_delegatesToRepository() {
        when(repository.findDistinctBatchIds()).thenReturn(List.of("BATCH-001", "BATCH-002"));

        List<String> ids = service.listBatchIds();

        assertThat(ids).containsExactly("BATCH-001", "BATCH-002");
    }

    @Test
    @DisplayName("getRecordsForBatch delegates to repository")
    void getRecordsForBatch_delegatesToRepository() {
        TransferRecord record = new TransferRecord();
        record.setBatchId("BATCH-001");
        when(repository.findByBatchId("BATCH-001")).thenReturn(List.of(record));

        List<TransferRecord> records = service.getRecordsForBatch("BATCH-001");

        assertThat(records).hasSize(1);
        assertThat(records.get(0).getBatchId()).isEqualTo("BATCH-001");
    }
}
