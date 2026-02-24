package com.wallaceespindola.resilience4jdemo.web;

import com.wallaceespindola.resilience4jdemo.service.CacheDemoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CacheController.class)
@Import(com.wallaceespindola.resilience4jdemo.util.GlobalExceptionHandler.class)
@DisplayName("CacheController Tests")
class CacheControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean CacheDemoService service;

    @BeforeEach
    void setUp() {
        when(service.getStats()).thenReturn(Map.of("hits", 2L, "misses", 1L));
    }

    @Test
    @DisplayName("GET /api/cache/metadata/region returns 200 with value")
    void getMetadata_returns200() throws Exception {
        when(service.getMetadata("region")).thenReturn("EU-WEST-1");

        mockMvc.perform(get("/api/cache/metadata/region"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.key").value("region"))
                .andExpect(jsonPath("$.data.value").value("EU-WEST-1"))
                .andExpect(jsonPath("$.data.hits").exists());
    }

    @Test
    @DisplayName("GET /api/cache/all returns 200 with all metadata")
    void getAll_returns200() throws Exception {
        when(service.getAllMetadata()).thenReturn(Map.of("region", "EU-WEST-1", "env", "demo"));

        mockMvc.perform(get("/api/cache/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.region").value("EU-WEST-1"));
    }

    @Test
    @DisplayName("GET /api/cache/clear calls service.clearCache and returns stats")
    void clearCache_get_returns200() throws Exception {
        mockMvc.perform(get("/api/cache/clear"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cache cleared"));
        verify(service).clearCache();
    }

    @Test
    @DisplayName("POST /api/cache/clear also clears cache")
    void clearCache_post_returns200() throws Exception {
        mockMvc.perform(post("/api/cache/clear"))
                .andExpect(status().isOk());
        verify(service).clearCache();
    }

    @Test
    @DisplayName("GET /api/cache/stats returns 200 with hit/miss data")
    void stats_returns200() throws Exception {
        mockMvc.perform(get("/api/cache/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hits").value(2))
                .andExpect(jsonPath("$.data.misses").value(1));
    }
}
