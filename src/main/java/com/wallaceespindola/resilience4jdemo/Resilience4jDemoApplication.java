package com.wallaceespindola.resilience4jdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Resilience4J Demo Application.
 *
 * <p>Demonstrates all six Resilience4J core modules—CircuitBreaker, Retry, RateLimiter,
 * Bulkhead, TimeLimiter, and Cache—through a realistic bulk-transfer scenario with
 * configurable fault injection.
 *
 * @author Wallace Espindola (wallace.espindola@gmail.com)
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
public class Resilience4jDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(Resilience4jDemoApplication.class, args);
    }
}
