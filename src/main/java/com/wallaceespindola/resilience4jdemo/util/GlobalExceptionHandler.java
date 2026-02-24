package com.wallaceespindola.resilience4jdemo.util;

import com.wallaceespindola.resilience4jdemo.dto.ApiResponse;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.concurrent.TimeoutException;

/** Translates Resilience4J and other exceptions into structured JSON responses. */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(CallNotPermittedException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiResponse<Void> handleCircuitOpen(CallNotPermittedException ex, HttpServletRequest req) {
        log.warn("CircuitBreaker OPEN — call rejected: {}", ex.getMessage());
        return ApiResponse.error("Circuit breaker is OPEN — downstream is unavailable",
                correlationId(req), req.getRequestURI());
    }

    @ExceptionHandler(BulkheadFullException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ApiResponse<Void> handleBulkheadFull(BulkheadFullException ex, HttpServletRequest req) {
        log.warn("Bulkhead full: {}", ex.getMessage());
        return ApiResponse.error("Bulkhead full — too many concurrent calls",
                correlationId(req), req.getRequestURI());
    }

    @ExceptionHandler(RequestNotPermitted.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ApiResponse<Void> handleRateLimited(RequestNotPermitted ex, HttpServletRequest req) {
        log.warn("Rate limited: {}", ex.getMessage());
        return ApiResponse.error("Rate limit exceeded — slow down",
                correlationId(req), req.getRequestURI());
    }

    @ExceptionHandler(TimeoutException.class)
    @ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
    public ApiResponse<Void> handleTimeout(TimeoutException ex, HttpServletRequest req) {
        log.warn("TimeLimiter timeout: {}", ex.getMessage());
        return ApiResponse.error("Request timed out",
                correlationId(req), req.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception at {}: {}", req.getRequestURI(), ex.getMessage(), ex);
        return ApiResponse.error(ex.getMessage(), correlationId(req), req.getRequestURI());
    }

    private String correlationId(HttpServletRequest req) {
        Object cid = req.getAttribute(CorrelationIdFilter.CORRELATION_ID_ATTR);
        return cid != null ? cid.toString() : "n/a";
    }
}
