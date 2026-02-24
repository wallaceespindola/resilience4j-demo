package com.wallaceespindola.resilience4jdemo.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallaceespindola.resilience4jdemo.dto.TransferRequest;
import com.wallaceespindola.resilience4jdemo.dto.TransferSummary;
import com.wallaceespindola.resilience4jdemo.service.TransferService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransferController.class)
@Import(com.wallaceespindola.resilience4jdemo.util.GlobalExceptionHandler.class)
@DisplayName("TransferController Tests")
class TransferControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean TransferService service;

    private TransferSummary buildSummary() {
        return TransferSummary.builder()
                .batchId("BATCH-TEST01")
                .totalRequested(10).pagesAttempted(2).pagesSucceeded(2).pagesFailed(0)
                .recordsInserted(10).retriesTotal(0).fallbacksUsed(0)
                .circuitBreakerRejections(0).bulkheadRejections(0)
                .rateLimiterRejections(0).timeoutRejections(0)
                .durationMs(42L)
                .build();
    }

    @Test
    @DisplayName("POST /api/transfer/start returns 200 with summary")
    void postStart_returns200() throws Exception {
        when(service.transfer(anyInt(), anyInt())).thenReturn(buildSummary());
        TransferRequest req = new TransferRequest(10, 5);

        mockMvc.perform(post("/api/transfer/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.data.batchId").value("BATCH-TEST01"))
                .andExpect(jsonPath("$.data.recordsInserted").value(10));
    }

    @Test
    @DisplayName("GET /api/transfer/start/{n}/{pageSize} returns 200")
    void getStart_returns200() throws Exception {
        when(service.transfer(anyInt(), anyInt())).thenReturn(buildSummary());

        mockMvc.perform(get("/api/transfer/start/10/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.batchId").value("BATCH-TEST01"));
    }

    @Test
    @DisplayName("GET /api/transfer/history returns batch id list")
    void getHistory_returnsList() throws Exception {
        when(service.listBatchIds()).thenReturn(List.of("BATCH-A", "BATCH-B"));

        mockMvc.perform(get("/api/transfer/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value("BATCH-A"))
                .andExpect(jsonPath("$.data[1]").value("BATCH-B"));
    }

    @Test
    @DisplayName("GET /api/transfer/batch/{id} returns records")
    void getBatch_returnsRecords() throws Exception {
        when(service.getRecordsForBatch("BATCH-A")).thenReturn(List.of());

        mockMvc.perform(get("/api/transfer/batch/BATCH-A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("Response always includes correlationId")
    void response_includesCorrelationId() throws Exception {
        when(service.transfer(anyInt(), anyInt())).thenReturn(buildSummary());

        mockMvc.perform(post("/api/transfer/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransferRequest(10, 5))))
                .andExpect(jsonPath("$.correlationId").isNotEmpty())
                .andExpect(header().exists("X-Correlation-Id"));
    }
}
