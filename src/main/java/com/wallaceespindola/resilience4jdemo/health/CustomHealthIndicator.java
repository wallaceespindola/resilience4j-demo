package com.wallaceespindola.resilience4jdemo.health;

import com.wallaceespindola.resilience4jdemo.fault.FaultInjectionSettings;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Custom health indicator that augments /actuator/health with:
 * - a {@code timestamp} field (required by project standards)
 * - current circuit-breaker state
 * - active fault-injection summary
 */
@Component("resilience4jDemo")
public class CustomHealthIndicator implements HealthIndicator {

    private final CircuitBreakerRegistry cbRegistry;
    private final FaultInjectionSettings faultSettings;

    public CustomHealthIndicator(CircuitBreakerRegistry cbRegistry,
                                 FaultInjectionSettings faultSettings) {
        this.cbRegistry   = cbRegistry;
        this.faultSettings = faultSettings;
    }

    @Override
    public Health health() {
        CircuitBreaker cb = cbRegistry.circuitBreaker("downstream");
        CircuitBreaker.State state = cb.getState();

        boolean circuitOpen = state == CircuitBreaker.State.OPEN
                           || state == CircuitBreaker.State.FORCED_OPEN;

        Health.Builder builder = circuitOpen ? Health.down() : Health.up();

        return builder
                .withDetail("timestamp",           Instant.now().toString())
                .withDetail("circuitBreakerState",  state.name())
                .withDetail("faultErrorRate",       faultSettings.getErrorRate() + "%")
                .withDetail("faultFixedDelayMs",    faultSettings.getFixedDelayMs())
                .withDetail("forceTimeout",         faultSettings.isForceTimeout())
                .withDetail("forceHttp500",         faultSettings.isForceHttp500())
                .withDetail("chaosMode",            faultSettings.isChaosMode())
                .build();
    }
}
