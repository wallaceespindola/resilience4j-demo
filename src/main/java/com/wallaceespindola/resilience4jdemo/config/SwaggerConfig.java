package com.wallaceespindola.resilience4jdemo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Resilience4J Demo API")
                        .version("1.0.0")
                        .description("""
                                Visual training app for all six Resilience4J core modules.
                                Use fault-injection endpoints to trigger failures, then observe
                                how CircuitBreaker, Retry, RateLimiter, Bulkhead, TimeLimiter,
                                and Cache protect the application.
                                """)
                        .contact(new Contact()
                                .name("Wallace Espindola")
                                .email("wallace.espindola@gmail.com")
                                .url("https://github.com/wallaceespindola/resilience4j-demo"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .tags(List.of(
                        new Tag().name("Fault Injection").description("Control downstream failure modes"),
                        new Tag().name("Transfer").description("Bulk-transfer scenario using all R4J modules"),
                        new Tag().name("Circuit Breaker").description("Demo endpoints for CircuitBreaker"),
                        new Tag().name("Retry").description("Demo endpoints for Retry"),
                        new Tag().name("Rate Limiter").description("Demo endpoints for RateLimiter"),
                        new Tag().name("Bulkhead").description("Demo endpoints for Bulkhead"),
                        new Tag().name("Time Limiter").description("Demo endpoints for TimeLimiter"),
                        new Tag().name("Cache").description("Demo endpoints for Cache"),
                        new Tag().name("Metrics").description("Resilience4J metrics snapshot")
                ));
    }
}
