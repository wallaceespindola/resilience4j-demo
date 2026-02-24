package com.wallaceespindola.resilience4jdemo.web;

import com.wallaceespindola.resilience4jdemo.dto.ApiResponse;
import com.wallaceespindola.resilience4jdemo.dto.ResilienceMetricsDto;
import com.wallaceespindola.resilience4jdemo.service.ResilienceMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes a single snapshot of all six R4J module metrics.
 * The dashboard polls this endpoint every 2 seconds to update the UI.
 */
@RestController
@RequestMapping("/api/metrics")
@Tag(name = "Metrics")
@RequiredArgsConstructor
public class MetricsController {

    private final ResilienceMetricsService service;

    @GetMapping("/resilience")
    @Operation(summary = "Snapshot of all Resilience4J module metrics")
    public ApiResponse<ResilienceMetricsDto> snapshot(HttpServletRequest req) {
        return ApiResponse.ok(service.snapshot(), cid(req), req.getRequestURI());
    }

    private String cid(HttpServletRequest req) {
        Object c = req.getAttribute("correlationId"); return c != null ? c.toString() : "n/a";
    }
}
