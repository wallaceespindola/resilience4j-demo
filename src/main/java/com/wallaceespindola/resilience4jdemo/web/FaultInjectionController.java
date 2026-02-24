package com.wallaceespindola.resilience4jdemo.web;

import com.wallaceespindola.resilience4jdemo.dto.ApiResponse;
import com.wallaceespindola.resilience4jdemo.dto.FaultSettingsDto;
import com.wallaceespindola.resilience4jdemo.fault.FaultInjectionSettings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * Controls fault-injection settings for the simulated downstream API.
 *
 * <p>Preset endpoints respond to both GET (browser/test-console friendly)
 * and POST (Postman / form submissions).
 */
@RestController
@RequestMapping("/api/fault")
@Tag(name = "Fault Injection")
@RequiredArgsConstructor
public class FaultInjectionController {

    private final FaultInjectionSettings settings;

    @GetMapping("/settings")
    @Operation(summary = "Get current fault settings")
    public ApiResponse<FaultSettingsDto> getSettings(HttpServletRequest req) {
        return ApiResponse.ok(settings.toDto(), cid(req), req.getRequestURI());
    }

    @PostMapping("/settings")
    @Operation(summary = "Apply custom fault settings")
    public ApiResponse<FaultSettingsDto> applySettings(@RequestBody FaultSettingsDto dto,
                                                        HttpServletRequest req) {
        settings.applyFrom(dto);
        return ApiResponse.ok(settings.toDto(), "Settings applied", cid(req), req.getRequestURI());
    }

    @RequestMapping(value = "/reset", method = {GET, POST})
    @Operation(summary = "Reset to healthy (no faults)")
    public ApiResponse<FaultSettingsDto> reset(HttpServletRequest req) {
        settings.reset();
        return ApiResponse.ok(settings.toDto(), "Reset to healthy", cid(req), req.getRequestURI());
    }

    @RequestMapping(value = "/flaky", method = {GET, POST})
    @Operation(summary = "Set 50% random error rate")
    public ApiResponse<FaultSettingsDto> flaky(HttpServletRequest req) {
        settings.applyFlaky();
        return ApiResponse.ok(settings.toDto(), "Flaky mode ON (50% failures)", cid(req), req.getRequestURI());
    }

    @RequestMapping(value = "/slow", method = {GET, POST})
    @Operation(summary = "Add 2-second fixed delay to every call")
    public ApiResponse<FaultSettingsDto> slow(HttpServletRequest req) {
        settings.applySlow();
        return ApiResponse.ok(settings.toDto(), "Slow mode ON (2000ms delay)", cid(req), req.getRequestURI());
    }

    @RequestMapping(value = "/timeout", method = {GET, POST})
    @Operation(summary = "Force timeout (3s delay > 1.5s limit)")
    public ApiResponse<FaultSettingsDto> forceTimeout(HttpServletRequest req) {
        settings.applyTimeout();
        return ApiResponse.ok(settings.toDto(), "Timeout mode ON", cid(req), req.getRequestURI());
    }

    @RequestMapping(value = "/http500", method = {GET, POST})
    @Operation(summary = "Force every call to return HTTP 500")
    public ApiResponse<FaultSettingsDto> forceHttp500(HttpServletRequest req) {
        settings.applyHttp500();
        return ApiResponse.ok(settings.toDto(), "Force HTTP 500 ON", cid(req), req.getRequestURI());
    }

    @RequestMapping(value = "/chaos", method = {GET, POST})
    @Operation(summary = "Enable chaos mode (mixed failures + delays)")
    public ApiResponse<FaultSettingsDto> chaos(HttpServletRequest req) {
        settings.applyChaos();
        return ApiResponse.ok(settings.toDto(), "Chaos mode ON", cid(req), req.getRequestURI());
    }

    /** Sets a specific error rate (0–100) via path variable — browser friendly. */
    @GetMapping("/error-rate/{rate}")
    @Operation(summary = "Set error rate 0–100%")
    public ApiResponse<FaultSettingsDto> setErrorRate(@PathVariable int rate, HttpServletRequest req) {
        settings.setErrorRate(Math.min(100, Math.max(0, rate)));
        return ApiResponse.ok(settings.toDto(), "Error rate set to " + settings.getErrorRate() + "%",
                cid(req), req.getRequestURI());
    }

    /** Sets a fixed delay via path variable — browser friendly. */
    @GetMapping("/delay/{ms}")
    @Operation(summary = "Set fixed delay in milliseconds")
    public ApiResponse<FaultSettingsDto> setDelay(@PathVariable long ms, HttpServletRequest req) {
        settings.setFixedDelayMs(Math.max(0, ms));
        return ApiResponse.ok(settings.toDto(), "Fixed delay set to " + settings.getFixedDelayMs() + "ms",
                cid(req), req.getRequestURI());
    }

    private String cid(HttpServletRequest req) {
        Object c = req.getAttribute("correlationId");
        return c != null ? c.toString() : "n/a";
    }
}
