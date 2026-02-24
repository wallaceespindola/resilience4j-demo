package com.wallaceespindola.resilience4jdemo.web;

import com.wallaceespindola.resilience4jdemo.dto.ApiResponse;
import com.wallaceespindola.resilience4jdemo.service.CacheDemoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.util.Map;

/**
 * Demonstrates the Resilience4J Cache pattern.
 *
 * <p>How to reproduce:
 * 1. Call {@code GET /api/cache/metadata/region} twice — first = miss, second = hit
 * 2. Enable error rate: {@code GET /api/fault/http500}
 * 3. Call again — cached result is returned even though downstream would fail
 * 4. Call {@code GET /api/cache/clear} to reset
 * 5. Next call will miss again (and now fail if 500 mode is on)
 */
@RestController
@RequestMapping("/api/cache")
@Tag(name = "Cache")
@RequiredArgsConstructor
public class CacheController {

    private final CacheDemoService service;

    @GetMapping("/metadata/{key}")
    @Operation(summary = "Fetch metadata by key (cached)")
    public ApiResponse<Map<String, String>> getMetadata(@PathVariable String key,
                                                         HttpServletRequest req) {
        String value = service.getMetadata(key);
        var stats    = service.getStats();
        return ApiResponse.ok(
                Map.of("key", key, "value", value,
                        "hits", stats.get("hits").toString(),
                        "misses", stats.get("misses").toString()),
                cid(req), req.getRequestURI());
    }

    @GetMapping("/all")
    @Operation(summary = "Fetch all metadata keys (batch cache demo)")
    public ApiResponse<Map<String, String>> getAll(HttpServletRequest req) {
        return ApiResponse.ok(service.getAllMetadata(), cid(req), req.getRequestURI());
    }

    @RequestMapping(value = "/clear", method = {RequestMethod.GET, RequestMethod.POST})
    @Operation(summary = "Clear the metadata cache and reset hit/miss counters")
    public ApiResponse<Map<String, Long>> clearCache(HttpServletRequest req) {
        service.clearCache();
        return ApiResponse.ok(service.getStats(), "Cache cleared", cid(req), req.getRequestURI());
    }

    @GetMapping("/stats")
    @Operation(summary = "Get cache hit/miss statistics")
    public ApiResponse<Map<String, Long>> stats(HttpServletRequest req) {
        return ApiResponse.ok(service.getStats(), cid(req), req.getRequestURI());
    }

    private String cid(HttpServletRequest req) {
        Object c = req.getAttribute("correlationId"); return c != null ? c.toString() : "n/a";
    }
}
