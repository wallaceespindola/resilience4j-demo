package com.wallaceespindola.resilience4jdemo.web;

import com.wallaceespindola.resilience4jdemo.dto.ResilienceMetricsDto;
import com.wallaceespindola.resilience4jdemo.service.ResilienceMetricsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MetricsController.class)
@Import(com.wallaceespindola.resilience4jdemo.util.GlobalExceptionHandler.class)
@DisplayName("MetricsController Tests")
class MetricsControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean ResilienceMetricsService service;

    @Test
    @DisplayName("GET /api/metrics/resilience returns 200 with snapshot")
    void snapshot_returns200() throws Exception {
        ResilienceMetricsDto dto = ResilienceMetricsDto.builder()
                .circuitBreaker(new ResilienceMetricsDto.CircuitBreakerMetrics("CLOSED", 0f, 0f, 0, 0, 0))
                .retry(new ResilienceMetricsDto.RetryMetrics(0, 0, 0, 0))
                .rateLimiter(new ResilienceMetricsDto.RateLimiterMetrics(5, 0))
                .bulkhead(new ResilienceMetricsDto.BulkheadMetrics(5, 5))
                .cache(new ResilienceMetricsDto.CacheMetrics(0, 0, 0))
                .build();
        when(service.snapshot()).thenReturn(dto);

        mockMvc.perform(get("/api/metrics/resilience"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.circuitBreaker.state").value("CLOSED"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/metrics/resilience includes correlationId header")
    void snapshot_hasCorrelationIdHeader() throws Exception {
        when(service.snapshot()).thenReturn(ResilienceMetricsDto.builder()
                .circuitBreaker(new ResilienceMetricsDto.CircuitBreakerMetrics("CLOSED", 0f, 0f, 0, 0, 0))
                .retry(new ResilienceMetricsDto.RetryMetrics(0, 0, 0, 0))
                .rateLimiter(new ResilienceMetricsDto.RateLimiterMetrics(5, 0))
                .bulkhead(new ResilienceMetricsDto.BulkheadMetrics(5, 5))
                .cache(new ResilienceMetricsDto.CacheMetrics(0, 0, 0))
                .build());

        mockMvc.perform(get("/api/metrics/resilience"))
                .andExpect(header().exists("X-Correlation-Id"));
    }
}
