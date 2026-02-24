package com.wallaceespindola.resilience4jdemo.web;

import com.wallaceespindola.resilience4jdemo.dto.DemoCallResult;
import com.wallaceespindola.resilience4jdemo.service.TimeLimiterDemoService;
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

@WebMvcTest(TimeLimiterController.class)
@Import(com.wallaceespindola.resilience4jdemo.util.GlobalExceptionHandler.class)
@DisplayName("TimeLimiterController Tests")
class TimeLimiterControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean TimeLimiterDemoService service;

    @Test
    @DisplayName("GET /api/time-limiter/call returns 200 on success")
    void call_returns200() throws Exception {
        when(service.call()).thenReturn(
                DemoCallResult.success("TimeLimiter", "ok", 1, 100, "CLOSED"));

        mockMvc.perform(get("/api/time-limiter/call"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.module").value("TimeLimiter"));
    }

    @Test
    @DisplayName("GET /api/time-limiter/call returns 200 with timeout outcome")
    void call_timeoutOutcome_returns200() throws Exception {
        when(service.call()).thenReturn(
                DemoCallResult.timeout("TimeLimiter", "timed out", 200, "CLOSED"));

        mockMvc.perform(get("/api/time-limiter/call"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.outcome").value("timeout"));
    }

    @Test
    @DisplayName("GET /api/time-limiter/call includes timestamp in response")
    void call_hasTimestamp() throws Exception {
        when(service.call()).thenReturn(
                DemoCallResult.success("TimeLimiter", "ok", 1, 100, "CLOSED"));

        mockMvc.perform(get("/api/time-limiter/call"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }
}
