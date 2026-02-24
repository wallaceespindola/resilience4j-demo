package com.wallaceespindola.resilience4jdemo.web;

import com.wallaceespindola.resilience4jdemo.dto.ApiResponse;
import com.wallaceespindola.resilience4jdemo.dto.DemoCallResult;
import com.wallaceespindola.resilience4jdemo.service.RateLimiterDemoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Demonstrates the RateLimiter pattern.
 *
 * <p>How to reproduce:
 * 1. Call {@code GET /api/rate-limiter/spam/20}
 * 2. The first 5 calls succeed, the rest are rejected (limitForPeriod=5/s)
 * 3. Wait 1 second and try again — the bucket refills
 */
@RestController
@RequestMapping("/api/rate-limiter")
@Tag(name = "Rate Limiter")
@RequiredArgsConstructor
public class RateLimiterController {

    private final RateLimiterDemoService service;

    @GetMapping("/call")
    @Operation(summary = "Single call through RateLimiter")
    public ApiResponse<DemoCallResult> call(HttpServletRequest req) {
        return ApiResponse.ok(service.call(), cid(req), req.getRequestURI());
    }

    @GetMapping("/spam/{n}")
    @Operation(summary = "Fire N rapid calls (use n=20 to see rejections)")
    public ApiResponse<List<DemoCallResult>> spam(@PathVariable int n, HttpServletRequest req) {
        return ApiResponse.ok(service.spam(Math.min(n, 50)), cid(req), req.getRequestURI());
    }

    @GetMapping("/metrics")
    @Operation(summary = "Current RateLimiter metrics")
    public ApiResponse<Map<String, Object>> metrics(HttpServletRequest req) {
        var m = service.getMetrics();
        return ApiResponse.ok(Map.of(
                "availablePermissions", m.getAvailablePermissions(),
                "waitingThreads",       m.getNumberOfWaitingThreads()
        ), cid(req), req.getRequestURI());
    }

    private String cid(HttpServletRequest req) {
        Object c = req.getAttribute("correlationId"); return c != null ? c.toString() : "n/a";
    }
}
