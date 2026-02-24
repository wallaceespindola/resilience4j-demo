package com.wallaceespindola.resilience4jdemo.web;

import com.wallaceespindola.resilience4jdemo.dto.DemoCallResult;
import com.wallaceespindola.resilience4jdemo.service.BulkheadDemoService;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BulkheadController.class)
@Import(com.wallaceespindola.resilience4jdemo.util.GlobalExceptionHandler.class)
@DisplayName("BulkheadController Tests")
class BulkheadControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean BulkheadDemoService service;

    private final DemoCallResult successResult =
            DemoCallResult.success("Bulkhead", "ok", 1, 5, "CLOSED");

    @BeforeEach
    void setUp() throws InterruptedException {
        when(service.call()).thenReturn(successResult);
        when(service.concurrent(anyInt())).thenReturn(List.of(successResult));
    }

    @Test
    @DisplayName("GET /api/bulkhead/call returns 200")
    void call_returns200() throws Exception {
        mockMvc.perform(get("/api/bulkhead/call"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.module").value("Bulkhead"));
    }

    @Test
    @DisplayName("GET /api/bulkhead/concurrent/3 returns 200 with list")
    void concurrent_returns200() throws Exception {
        mockMvc.perform(get("/api/bulkhead/concurrent/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/bulkhead/metrics returns 200 with metric data")
    void metrics_returns200() throws Exception {
        Bulkhead bh = Bulkhead.of("test", BulkheadConfig.ofDefaults());
        when(service.getMetrics()).thenReturn(bh.getMetrics());

        mockMvc.perform(get("/api/bulkhead/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.availableConcurrentCalls").exists());
    }

    @Test
    @DisplayName("GET /api/bulkhead/call — bulkhead full returns 429")
    void call_bulkheadFull_returns429() throws Exception {
        Bulkhead bh = Bulkhead.of("full", BulkheadConfig.custom()
                .maxConcurrentCalls(0).build());
        when(service.call()).thenThrow(BulkheadFullException.createBulkheadFullException(bh));

        mockMvc.perform(get("/api/bulkhead/call"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value("error"));
    }
}
