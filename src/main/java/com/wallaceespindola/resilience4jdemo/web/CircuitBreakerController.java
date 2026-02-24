package com.wallaceespindola.resilience4jdemo.web;

import com.wallaceespindola.resilience4jdemo.dto.ApiResponse;
import com.wallaceespindola.resilience4jdemo.dto.DemoCallResult;
import com.wallaceespindola.resilience4jdemo.service.CircuitBreakerDemoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Demonstrates the CircuitBreaker pattern.
 *
 * <p>How to reproduce OPEN state:
 * 1. Set fault: {@code GET /api/fault/http500}
 * 2. Call {@code GET /api/cb/call} 10+ times (> failure threshold)
 * 3. Watch the state transition to OPEN via {@code GET /api/cb/state}
 * 4. Reset fault and watch it recover to HALF_OPEN then CLOSED
 */
@RestController
@RequestMapping("/api/cb")
@Tag(name = "Circuit Breaker")
@RequiredArgsConstructor
public class CircuitBreakerController {

    private final CircuitBreakerDemoService service;

    @GetMapping("/call")
    @Operation(summary = "Make one call through the CircuitBreaker")
    public ApiResponse<DemoCallResult> call(HttpServletRequest req) {
        return ApiResponse.ok(service.call(), cid(req), req.getRequestURI());
    }

    /** Fires n calls rapidly to build up failure rate and trip the breaker. */
    @GetMapping("/spam/{n}")
    @Operation(summary = "Fire N calls to drive up failure rate")
    public ApiResponse<List<DemoCallResult>> spam(@PathVariable int n, HttpServletRequest req) {
        List<DemoCallResult> results = new ArrayList<>();
        for (int i = 0; i < Math.min(n, 50); i++) results.add(service.call());
        return ApiResponse.ok(results, cid(req), req.getRequestURI());
    }

    @GetMapping("/state")
    @Operation(summary = "Get current CircuitBreaker state and metrics")
    public ApiResponse<Map<String, Object>> state(HttpServletRequest req) {
        var m = service.getMetrics();
        var data = Map.<String, Object>of(
                "state",                service.getState(),
                "failureRate",          m.getFailureRate(),
                "slowCallRate",         m.getSlowCallRate(),
                "successfulCalls",      m.getNumberOfSuccessfulCalls(),
                "failedCalls",          m.getNumberOfFailedCalls(),
                "notPermittedCalls",    m.getNumberOfNotPermittedCalls(),
                "bufferedCalls",        m.getNumberOfBufferedCalls()
        );
        return ApiResponse.ok(data, cid(req), req.getRequestURI());
    }

    @RequestMapping(value = "/force-open", method = {RequestMethod.GET, RequestMethod.POST})
    @Operation(summary = "Manually force the circuit breaker OPEN")
    public ApiResponse<String> forceOpen(HttpServletRequest req) {
        service.forceOpen();
        return ApiResponse.ok(service.getState(), "Circuit forced OPEN", cid(req), req.getRequestURI());
    }

    @RequestMapping(value = "/reset", method = {RequestMethod.GET, RequestMethod.POST})
    @Operation(summary = "Reset circuit breaker to CLOSED")
    public ApiResponse<String> reset(HttpServletRequest req) {
        service.reset();
        return ApiResponse.ok(service.getState(), "Circuit reset to CLOSED", cid(req), req.getRequestURI());
    }

    private String cid(HttpServletRequest req) {
        Object c = req.getAttribute("correlationId"); return c != null ? c.toString() : "n/a";
    }
}
