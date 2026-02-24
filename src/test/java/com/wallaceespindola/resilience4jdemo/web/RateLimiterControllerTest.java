package com.wallaceespindola.resilience4jdemo.web;

import com.wallaceespindola.resilience4jdemo.dto.DemoCallResult;
import com.wallaceespindola.resilience4jdemo.service.RateLimiterDemoService;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RateLimiterController.class)
@Import(com.wallaceespindola.resilience4jdemo.util.GlobalExceptionHandler.class)
@DisplayName("RateLimiterController Tests")
class RateLimiterControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean RateLimiterDemoService service;

    @BeforeEach
    void setUp() {
        when(service.call()).thenReturn(
                DemoCallResult.success("RateLimiter", "ok", 1, 2, "CLOSED"));
        when(service.spam(anyInt())).thenReturn(List.of(
                DemoCallResult.success("RateLimiter", "ok", 1, 2, "CLOSED")));
    }

    @Test
    @DisplayName("GET /api/rate-limiter/call returns 200")
    void call_returns200() throws Exception {
        mockMvc.perform(get("/api/rate-limiter/call"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.module").value("RateLimiter"));
    }

    @Test
    @DisplayName("GET /api/rate-limiter/spam/5 returns 200 with list")
    void spam_returns200WithList() throws Exception {
        mockMvc.perform(get("/api/rate-limiter/spam/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/rate-limiter/metrics returns 200 with permit data")
    void metrics_returns200() throws Exception {
        RateLimiter rl = RateLimiterRegistry.of(RateLimiterConfig.custom()
                .limitForPeriod(10)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofMillis(100))
                .build()).rateLimiter("test");
        when(service.getMetrics()).thenReturn(rl.getMetrics());

        mockMvc.perform(get("/api/rate-limiter/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.availablePermissions").exists());
    }
}
