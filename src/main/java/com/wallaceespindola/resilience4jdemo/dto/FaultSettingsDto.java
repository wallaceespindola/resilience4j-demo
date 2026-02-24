package com.wallaceespindola.resilience4jdemo.dto;

/** Snapshot / update payload for fault-injection settings. */
public record FaultSettingsDto(
        int errorRate,
        long fixedDelayMs,
        long randomDelayMaxMs,
        boolean forceTimeout,
        boolean forceHttp500,
        boolean rateLimitMode,
        int maxConcurrentDownstream,
        boolean chaosMode
) {}
