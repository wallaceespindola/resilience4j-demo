package com.wallaceespindola.resilience4jdemo.web;

import com.wallaceespindola.resilience4jdemo.dto.ApiResponse;
import com.wallaceespindola.resilience4jdemo.dto.DemoCallResult;
import com.wallaceespindola.resilience4jdemo.service.BulkheadDemoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Demonstrates the Bulkhead (semaphore) pattern.
 *
 * <p>How to reproduce:
 * 1. Add a slow delay: {@code GET /api/fault/slow} (2s per call)
 * 2. Call {@code GET /api/bulkhead/concurrent/10}
 * 3. With maxConcurrentCalls=5 and a 2s delay, 5 calls will be admitted
 *    and the other 5 rejected immediately with "Bulkhead full"
 */
@RestController
@RequestMapping("/api/bulkhead")
@Tag(name = "Bulkhead")
@RequiredArgsConstructor
public class BulkheadController {

    private final BulkheadDemoService service;

    @GetMapping("/call")
    @Operation(summary = "Single call through Bulkhead")
    public ApiResponse<DemoCallResult> call(HttpServletRequest req) {
        return ApiResponse.ok(service.call(), cid(req), req.getRequestURI());
    }

    @GetMapping("/concurrent/{n}")
    @Operation(summary = "Fire N concurrent calls (use n=10 with slow mode)")
    public ApiResponse<List<DemoCallResult>> concurrent(@PathVariable int n,
                                                         HttpServletRequest req) throws InterruptedException {
        return ApiResponse.ok(service.concurrent(Math.min(n, 20)), cid(req), req.getRequestURI());
    }

    @GetMapping("/metrics")
    @Operation(summary = "Current Bulkhead metrics")
    public ApiResponse<Map<String, Object>> metrics(HttpServletRequest req) {
        var m = service.getMetrics();
        return ApiResponse.ok(Map.of(
                "availableConcurrentCalls", m.getAvailableConcurrentCalls(),
                "maxAllowedConcurrentCalls", m.getMaxAllowedConcurrentCalls()
        ), cid(req), req.getRequestURI());
    }

    private String cid(HttpServletRequest req) {
        Object c = req.getAttribute("correlationId"); return c != null ? c.toString() : "n/a";
    }
}
