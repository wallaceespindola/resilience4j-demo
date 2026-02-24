package com.wallaceespindola.resilience4jdemo.web;

import com.wallaceespindola.resilience4jdemo.dto.DemoCallResult;
import com.wallaceespindola.resilience4jdemo.service.CircuitBreakerDemoService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CircuitBreakerController.class)
@Import(com.wallaceespindola.resilience4jdemo.util.GlobalExceptionHandler.class)
@DisplayName("CircuitBreakerController Tests")
class CircuitBreakerControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean CircuitBreakerDemoService service;

    private final DemoCallResult successResult =
            DemoCallResult.success("CircuitBreaker", "ok", 1, 10, "CLOSED");

    @BeforeEach
    void setUp() {
        when(service.call()).thenReturn(successResult);
        when(service.getState()).thenReturn("CLOSED");
    }

    @Test
    @DisplayName("GET /api/cb/call returns 200 with success outcome")
    void call_returns200() throws Exception {
        mockMvc.perform(get("/api/cb/call"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.outcome").value("success"));
    }

    @Test
    @DisplayName("GET /api/cb/spam/3 returns 200 with list of results")
    void spam_returns200WithList() throws Exception {
        mockMvc.perform(get("/api/cb/spam/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
        verify(service, times(3)).call();
    }

    @Test
    @DisplayName("GET /api/cb/state returns 200 with CB state data")
    void state_returns200() throws Exception {
        CircuitBreaker.Metrics metrics =
                CircuitBreakerRegistry.of(CircuitBreakerConfig.ofDefaults())
                        .circuitBreaker("test").getMetrics();
        when(service.getMetrics()).thenReturn(metrics);

        mockMvc.perform(get("/api/cb/state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("CLOSED"));
    }

    @Test
    @DisplayName("GET /api/cb/force-open returns 200 and calls forceOpen")
    void forceOpen_returns200() throws Exception {
        mockMvc.perform(get("/api/cb/force-open"))
                .andExpect(status().isOk());
        verify(service).forceOpen();
    }

    @Test
    @DisplayName("GET /api/cb/reset returns 200 and calls reset")
    void reset_returns200() throws Exception {
        mockMvc.perform(get("/api/cb/reset"))
                .andExpect(status().isOk());
        verify(service).reset();
    }

    @Test
    @DisplayName("POST /api/cb/reset also returns 200")
    void reset_post_returns200() throws Exception {
        mockMvc.perform(post("/api/cb/reset"))
                .andExpect(status().isOk());
        verify(service).reset();
    }

    @Test
    @DisplayName("GET /api/cb/call — circuit open returns 503")
    void call_circuitOpen_returns503() throws Exception {
        CircuitBreaker testCb = CircuitBreakerRegistry.of(CircuitBreakerConfig.ofDefaults())
                .circuitBreaker("test-open");
        testCb.transitionToForcedOpenState();
        when(service.call()).thenThrow(
                io.github.resilience4j.circuitbreaker.CallNotPermittedException
                        .createCallNotPermittedException(testCb));

        mockMvc.perform(get("/api/cb/call"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("error"));
    }
}
