<div align="center">

<img src="https://avatars.githubusercontent.com/u/26137419" alt="Resilience4J" width="110" style="border-radius:12px"/>

# Resilience4J Demo

**Java 21 · Spring Boot 3 · Six fault-tolerance patterns in one project**

[![CI](https://github.com/wallaceespindola/resilience4j-demo/actions/workflows/ci.yml/badge.svg)](https://github.com/wallaceespindola/resilience4j-demo/actions/workflows/ci.yml)
[![CodeQL](https://github.com/wallaceespindola/resilience4j-demo/actions/workflows/codeql.yml/badge.svg)](https://github.com/wallaceespindola/resilience4j-demo/actions/workflows/codeql.yml)
[![Coverage](https://img.shields.io/badge/coverage-85%25-brightgreen)](https://github.com/wallaceespindola/resilience4j-demo)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Resilience4J](https://img.shields.io/badge/Resilience4J-2.2-29ABE2)](https://resilience4j.readme.io/)
[![Maven](https://img.shields.io/badge/Maven-3-C71A36?logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![Docker](https://img.shields.io/badge/Docker-ready-2496ED?logo=docker&logoColor=white)](Dockerfile)
[![JUnit5](https://img.shields.io/badge/JUnit-5-25A162?logo=junit5&logoColor=white)](https://junit.org/junit5/)
[![Swagger](https://img.shields.io/badge/Swagger-UI-85EA2D?logo=swagger&logoColor=black)](http://localhost:8080/swagger-ui.html)

</div>

---

A Java 21 / Spring Boot 3 project covering all six Resilience4J modules: CircuitBreaker,
Retry, RateLimiter, Bulkhead, TimeLimiter, and Cache.

Built around a bulk-transfer scenario where you push data through a simulated downstream
API — one you can deliberately break (slow responses, flaky errors, forced timeouts, HTTP 500)
and watch how each resilience pattern reacts. There's a test console and a live dashboard
included, so you can run experiments without writing a single curl command.

---

## What you get after starting the app

| URL                                     | What it is                                        |
|-----------------------------------------|---------------------------------------------------|
| `http://localhost:8080/`                | Test Console — one button per endpoint            |
| `http://localhost:8080/dashboard.html`  | Live Dashboard — Chart.js charts, polls every 2s  |
| `http://localhost:8080/swagger-ui.html` | Swagger UI — full API docs                        |
| `http://localhost:8080/actuator/health` | Health endpoint (includes CB state and timestamp) |
| `http://localhost:8080/actuator/info`   | App info (version, author)                        |
| `http://localhost:8080/h2-console`      | H2 in-memory DB browser                           |

---

## How to trigger each pattern

### CircuitBreaker

1. In the Fault panel, click **Force HTTP 500**
2. In the CircuitBreaker section, click **Spam 10** (or call `/api/cb/spam/10`)
3. Watch the state badge on the Dashboard change: `CLOSED` → `OPEN`
4. After 10 seconds it moves to `HALF_OPEN` and lets 3 probe calls through
5. **Reset Healthy** brings it back immediately

### Retry

1. Enable **Flaky 50%** in the Fault panel
2. Call `/api/retry/call` a few times
3. `attemptNumber > 1` in the response means a retry happened
4. Set error rate to 100% to exhaust all 3 attempts and see the fallback

### RateLimiter

1. Reset faults first
2. Click **Spam 20** in the Rate Limiter section
3. First 5 calls succeed, the remaining 15 come back as `rejected` (limit is 5/s)
4. Wait a second and try again — the bucket refills

### Bulkhead

1. Enable **Slow 2s** in the Fault panel
2. Click **10 Concurrent** in the Bulkhead section
3. 5 calls get through, the other 5 are rejected immediately (`maxConcurrentCalls=5`)

### TimeLimiter

1. Enable **Force Timeout** (injects a 3s delay; the limit is 1.5s)
2. Call `/api/time-limiter/call`
3. Response comes back with `outcome=timeout`
4. **Reset Healthy** shows a normal call finishing in a few milliseconds

### Cache

1. Call `/api/cache/metadata/region` twice
2. First call: `misses=1, hits=0` — downstream was called
3. Second call: `misses=1, hits=1` — served from cache, no downstream call
4. Now enable **Force HTTP 500**, call again — cached value still comes back fine
5. **Clear Cache**, then call again — this time it fails, because the cache is empty and the downstream is down

---

## Quick start

### Run locally

```bash
git clone https://github.com/wallaceespindola/resilience4j-demo.git
cd resilience4j-demo

./mvnw spring-boot:run
# or: make run

# App starts at http://localhost:8080
```

Java 21 is required.

### Tests

```bash
./mvnw test
# or: make test
```

102 tests across all modules (services, controllers, client, fault injection).

### Coverage report (enforces ≥80%)

```bash
./mvnw verify
# Report: target/site/jacoco/index.html
# or: make coverage
```

Current instruction coverage: **~85%**.

### Docker

```bash
# Build and start via docker-compose
make docker-up

# Or manually:
docker build -t resilience4j-demo .
docker run -p 8080:8080 resilience4j-demo
```

---

## Project layout

```
src/main/java/com/wallaceespindola/resilience4jdemo/
├── Resilience4jDemoApplication.java
├── config/
│   ├── CacheConfig.java                # Caffeine JCache + R4J Cache bean
│   └── SwaggerConfig.java              # OpenAPI configuration
├── domain/
│   └── TransferRecord.java             # JPA entity (stored in H2)
├── dto/                                # Java Records: ApiResponse, TransferSummary, etc.
├── fault/
│   └── FaultInjectionSettings.java     # Singleton; all downstream calls read this
├── client/
│   └── SimulatedDownstreamClient.java  # Applies faults and returns fake data
├── service/
│   ├── TransferService.java            # Bulk transfer — uses all 6 R4J modules
│   ├── CircuitBreakerDemoService.java
│   ├── RetryDemoService.java
│   ├── RateLimiterDemoService.java
│   ├── BulkheadDemoService.java
│   ├── TimeLimiterDemoService.java
│   ├── CacheDemoService.java
│   └── ResilienceMetricsService.java
├── web/                               # One REST controller per module + transfer + metrics
├── health/
│   └── CustomHealthIndicator.java     # Adds CB state and timestamp to /actuator/health
└── util/
    ├── CorrelationIdFilter.java       # Sets X-Correlation-Id on every request/response
    └── GlobalExceptionHandler.java    # Maps R4J exceptions to structured JSON responses

src/main/resources/
├── application.properties             # All R4J config, actuator, H2, OpenAPI
└── static/
    ├── index.html                     # Test Console
    └── dashboard.html                 # Live Dashboard with Chart.js

src/test/java/...
├── client/
│   └── ...
├── fault/
│   └── ...
├── service/
│   └── ...
└── web/
    └── ...
```

---

## API reference

All responses use the same envelope:

```json
{
  "data": {
    ...
  },
  "status": "success",
  "message": "optional note",
  "timestamp": "2026-02-24T10:30:00.123Z",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "path": "/api/transfer/start/100/10"
}
```

### Key endpoints

| Method | Path                                 | What it does                               |
|--------|--------------------------------------|--------------------------------------------|
| `GET`  | `/api/fault/reset`                   | Clear all fault settings                   |
| `GET`  | `/api/fault/flaky`                   | 50% random error rate                      |
| `GET`  | `/api/fault/slow`                    | 2s fixed delay                             |
| `GET`  | `/api/fault/timeout`                 | Force timeout (3s delay, limit 1.5s)       |
| `GET`  | `/api/fault/http500`                 | Force HTTP 500 on every call               |
| `GET`  | `/api/fault/chaos`                   | All of the above combined                  |
| `POST` | `/api/fault/settings`                | Set exact fault parameters as JSON         |
| `GET`  | `/api/transfer/start/{n}/{pageSize}` | Run a bulk transfer                        |
| `GET`  | `/api/cb/call`                       | One call through the CircuitBreaker        |
| `GET`  | `/api/cb/spam/{n}`                   | N quick calls to build up the failure rate |
| `GET`  | `/api/cb/state`                      | Current CB state and metrics               |
| `GET`  | `/api/retry/call`                    | One call with retry (up to 3 attempts)     |
| `GET`  | `/api/rate-limiter/spam/{n}`         | N rapid calls — shows rejections           |
| `GET`  | `/api/bulkhead/concurrent/{n}`       | N concurrent calls                         |
| `GET`  | `/api/time-limiter/call`             | Async call with a 1.5s timeout             |
| `GET`  | `/api/cache/metadata/{key}`          | Cached metadata lookup                     |
| `GET`  | `/api/metrics/resilience`            | Snapshot of all R4J metrics                |

### Example response — bulk transfer with HTTP 500 active

```json
GET /api/transfer/start/50/10

{
  "data": {
    "batchId": "BATCH-A1B2C3D4",
    "totalRequested": 50,
    "pagesAttempted": 5,
    "pagesSucceeded": 0,
    "pagesFailed": 5,
    "recordsInserted": 0,
    "retriesTotal": 10,
    "fallbacksUsed": 5,
    "circuitBreakerRejections": 0,
    "rateLimiterRejections": 0,
    "timeoutRejections": 0,
    "durationMs": 412,
    "timestamp": "2026-02-24T10:30:05.123Z"
  },
  "status": "success",
  "timestamp": "2026-02-24T10:30:05.125Z",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000"
}
```

---

## Resilience4J configuration

| Module         | Setting                   | Value                      |
|----------------|---------------------------|----------------------------|
| CircuitBreaker | `failureRateThreshold`    | 50%                        |
| CircuitBreaker | `slidingWindowSize`       | 10 calls                   |
| CircuitBreaker | `waitDurationInOpenState` | 10s                        |
| Retry          | `maxAttempts`             | 3                          |
| Retry          | `waitDuration`            | 300ms base, exponential ×2 |
| RateLimiter    | `limitForPeriod`          | 5 calls/s                  |
| RateLimiter    | `timeoutDuration`         | 200ms                      |
| Bulkhead       | `maxConcurrentCalls`      | 5                          |
| TimeLimiter    | `timeoutDuration`         | 1500ms                     |
| Cache          | TTL                       | 30s (Caffeine)             |

---

## CI/CD

| File                           | Purpose                                                |
|--------------------------------|--------------------------------------------------------|
| `.github/workflows/ci.yml`     | Build, test, JaCoCo coverage report, Docker smoke test |
| `.github/workflows/codeql.yml` | CodeQL Java security scan (weekly + on PRs)            |
| `.github/dependabot.yml`       | Weekly dependency updates for Maven and Actions        |

---

## Postman collection

Import `postman/resilience4j-demo.postman_collection.json` and set the `baseUrl` variable
to `http://localhost:8080`.

---

## Tech stack

- **Java 21** · **Spring Boot 3.4** · **Maven**
- **Resilience4J 2.2** — CircuitBreaker, Retry, RateLimiter, Bulkhead, TimeLimiter, Cache
- **Spring Data JPA + H2**
- **Caffeine + JCache** (backing store for R4J Cache)
- **Micrometer + Prometheus**
- **Springdoc OpenAPI 2.7** (Swagger UI)
- **Lombok** · **JUnit 5** · **Mockito** · **JaCoCo**
- **Chart.js** (dashboard)
- **Docker** · **GitHub Actions**

---

## Author

**Wallace Espindola** — Software Engineer Sr. / Solutions Architect / Java & Python Dev

- Email: [wallace.espindola@gmail.com](mailto:wallace.espindola@gmail.com)
- LinkedIn: [linkedin.com/in/wallaceespindola](https://www.linkedin.com/in/wallaceespindola/)
- GitHub: [github.com/wallaceespindola](https://github.com/wallaceespindola/)

---

*Licensed under Apache 2.0 — see [LICENSE](LICENSE)*
