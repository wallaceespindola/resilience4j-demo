package com.wallaceespindola.resilience4jdemo.web;

import com.wallaceespindola.resilience4jdemo.dto.ApiResponse;
import com.wallaceespindola.resilience4jdemo.dto.DemoCallResult;
import com.wallaceespindola.resilience4jdemo.service.TimeLimiterDemoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Demonstrates the TimeLimiter pattern.
 *
 * <p>How to reproduce:
 * 1. Enable timeout mode: {@code GET /api/fault/timeout}
 *    (adds 3-second delay; TimeLimiter cuts off at 1.5 s)
 * 2. Call {@code GET /api/time-limiter/call}
 * 3. Observe outcome=timeout and the fallback message
 * 4. Reset and call again to see normal completion
 */
@RestController
@RequestMapping("/api/time-limiter")
@Tag(name = "Time Limiter")
@RequiredArgsConstructor
public class TimeLimiterController {

    private final TimeLimiterDemoService service;

    @GetMapping("/call")
    @Operation(summary = "Make one async call with TimeLimiter (timeout=1500ms)")
    public ApiResponse<DemoCallResult> call(HttpServletRequest req) {
        return ApiResponse.ok(service.call(), cid(req), req.getRequestURI());
    }

    private String cid(HttpServletRequest req) {
        Object c = req.getAttribute("correlationId"); return c != null ? c.toString() : "n/a";
    }
}
