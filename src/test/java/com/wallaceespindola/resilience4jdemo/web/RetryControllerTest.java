package com.wallaceespindola.resilience4jdemo.web;

import com.wallaceespindola.resilience4jdemo.dto.DemoCallResult;
import com.wallaceespindola.resilience4jdemo.service.RetryDemoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RetryController.class)
@Import(com.wallaceespindola.resilience4jdemo.util.GlobalExceptionHandler.class)
@DisplayName("RetryController Tests")
class RetryControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean RetryDemoService service;

    @BeforeEach
    void setUp() {
        when(service.call()).thenReturn(
                DemoCallResult.success("Retry", "ok", 1, 5, "CLOSED"));
    }

    @Test
    @DisplayName("GET /api/retry/call returns 200 with result")
    void call_returns200() throws Exception {
        mockMvc.perform(get("/api/retry/call"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.module").value("Retry"));
    }

    @Test
    @DisplayName("GET /api/retry/spam/4 returns 200 with list")
    void spam_returns200WithList() throws Exception {
        mockMvc.perform(get("/api/retry/spam/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
        verify(service, times(4)).call();
    }

    @Test
    @DisplayName("spam caps at 20 calls")
    void spam_capsAt20() throws Exception {
        mockMvc.perform(get("/api/retry/spam/50"))
                .andExpect(status().isOk());
        verify(service, times(20)).call();
    }
}
