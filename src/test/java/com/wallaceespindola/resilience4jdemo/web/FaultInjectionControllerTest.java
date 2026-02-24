package com.wallaceespindola.resilience4jdemo.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallaceespindola.resilience4jdemo.dto.FaultSettingsDto;
import com.wallaceespindola.resilience4jdemo.fault.FaultInjectionSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FaultInjectionController.class)
@Import(com.wallaceespindola.resilience4jdemo.util.GlobalExceptionHandler.class)
@DisplayName("FaultInjectionController Tests")
class FaultInjectionControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean FaultInjectionSettings settings;

    private final FaultSettingsDto healthyDto =
            new FaultSettingsDto(0, 0, 0, false, false, false, 10, false);

    @BeforeEach
    void setUp() {
        when(settings.toDto()).thenReturn(healthyDto);
    }

    @Test
    @DisplayName("GET /api/fault/settings returns 200 with timestamp")
    void getSettings_returns200() throws Exception {
        mockMvc.perform(get("/api/fault/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.data.errorRate").value(0));
    }

    @Test
    @DisplayName("GET /api/fault/reset calls settings.reset()")
    void getReset_callsReset() throws Exception {
        mockMvc.perform(get("/api/fault/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Reset to healthy"));
        verify(settings).reset();
    }

    @Test
    @DisplayName("GET /api/fault/flaky calls settings.applyFlaky()")
    void getFlaky_callsApplyFlaky() throws Exception {
        mockMvc.perform(get("/api/fault/flaky"))
                .andExpect(status().isOk());
        verify(settings).applyFlaky();
    }

    @Test
    @DisplayName("GET /api/fault/slow calls settings.applySlow()")
    void getSlow_callsApplySlow() throws Exception {
        mockMvc.perform(get("/api/fault/slow"))
                .andExpect(status().isOk());
        verify(settings).applySlow();
    }

    @Test
    @DisplayName("GET /api/fault/timeout calls settings.applyTimeout()")
    void getTimeout_callsApplyTimeout() throws Exception {
        mockMvc.perform(get("/api/fault/timeout"))
                .andExpect(status().isOk());
        verify(settings).applyTimeout();
    }

    @Test
    @DisplayName("GET /api/fault/http500 calls settings.applyHttp500()")
    void getHttp500_callsApplyHttp500() throws Exception {
        mockMvc.perform(get("/api/fault/http500"))
                .andExpect(status().isOk());
        verify(settings).applyHttp500();
    }

    @Test
    @DisplayName("GET /api/fault/chaos calls settings.applyChaos()")
    void getChaos_callsApplyChaos() throws Exception {
        mockMvc.perform(get("/api/fault/chaos"))
                .andExpect(status().isOk());
        verify(settings).applyChaos();
    }

    @Test
    @DisplayName("POST /api/fault/settings applies custom dto")
    void postSettings_appliesCustomDto() throws Exception {
        FaultSettingsDto dto = new FaultSettingsDto(30, 500, 1000, false, false, false, 5, false);
        mockMvc.perform(post("/api/fault/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
        verify(settings).applyFrom(any(FaultSettingsDto.class));
    }

    @Test
    @DisplayName("GET /api/fault/error-rate/50 sets error rate")
    void getErrorRate_setsErrorRate() throws Exception {
        mockMvc.perform(get("/api/fault/error-rate/50"))
                .andExpect(status().isOk());
        verify(settings).setErrorRate(50);
    }

    @Test
    @DisplayName("Response includes X-Correlation-Id header")
    void response_includesCorrelationIdHeader() throws Exception {
        mockMvc.perform(get("/api/fault/settings"))
                .andExpect(header().exists("X-Correlation-Id"));
    }
}
