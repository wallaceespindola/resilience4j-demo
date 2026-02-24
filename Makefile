# Makefile for resilience4j-demo
# Delegates to Maven. Requires Java 21 and Maven on PATH (or uses ./mvnw).

MVN     := ./mvnw
APP_JAR := target/resilience4j-demo-*.jar
IMAGE   := resilience4j-demo

.PHONY: help setup run test coverage clean docker-build docker-up docker-down lint

help: ## Show available targets
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2}'

setup: ## Download all Maven dependencies
	$(MVN) dependency:resolve -q

run: ## Run the application (hot-reload via DevTools)
	$(MVN) spring-boot:run

test: ## Run all unit tests
	$(MVN) -B test

coverage: ## Run tests and generate JaCoCo HTML report (target/site/jacoco/index.html)
	$(MVN) -B verify
	@echo "Report: target/site/jacoco/index.html"

clean: ## Clean build artifacts
	$(MVN) clean

build: ## Compile and package (skip tests)
	$(MVN) clean package -DskipTests -q

docker-build: build ## Build Docker image
	docker build -t $(IMAGE):latest .

docker-up: docker-build ## Start with docker-compose
	docker compose up -d
	@echo "App:     http://localhost:8080"
	@echo "Swagger: http://localhost:8080/swagger-ui.html"
	@echo "Health:  http://localhost:8080/actuator/health"

docker-down: ## Stop docker-compose stack
	docker compose down

lint: ## Basic code checks (compile only)
	$(MVN) compile -q
