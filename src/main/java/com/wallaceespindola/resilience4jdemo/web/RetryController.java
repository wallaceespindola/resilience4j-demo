package com.wallaceespindola.resilience4jdemo.web;

import com.wallaceespindola.resilience4jdemo.dto.ApiResponse;
import com.wallaceespindola.resilience4jdemo.dto.DemoCallResult;
import com.wallaceespindola.resilience4jdemo.service.RetryDemoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates the Retry pattern.
 *
 * <p>How to reproduce:
 * 1. Set fault: {@code GET /api/fault/flaky} (50% error rate)
 * 2. Call {@code GET /api/retry/call} several times
 * 3. Watch attemptNumber in the response — it will be > 1 when retries occur
 * 4. Set errorRate=100% to see all 3 attempts exhausted and a fallback returned
 */
@RestController
@RequestMapping("/api/retry")
@Tag(name = "Retry")
@RequiredArgsConstructor
public class RetryController {

    private final RetryDemoService service;

    @GetMapping("/call")
    @Operation(summary = "Make one call with Retry (up to 3 attempts)")
    public ApiResponse<DemoCallResult> call(HttpServletRequest req) {
        return ApiResponse.ok(service.call(), cid(req), req.getRequestURI());
    }

    @GetMapping("/spam/{n}")
    @Operation(summary = "Fire N calls and collect all results with attempt counts")
    public ApiResponse<List<DemoCallResult>> spam(@PathVariable int n, HttpServletRequest req) {
        List<DemoCallResult> results = new ArrayList<>();
        for (int i = 0; i < Math.min(n, 20); i++) results.add(service.call());
        return ApiResponse.ok(results, cid(req), req.getRequestURI());
    }

    private String cid(HttpServletRequest req) {
        Object c = req.getAttribute("correlationId"); return c != null ? c.toString() : "n/a";
    }
}
