.PHONY: help infra-up infra-down build run-secrets run-gateway run-incident run-evidence run-activity run-frontend test-flow clean

# Default target
help:
	@echo "PulseOps v3.0 - Available Commands"
	@echo "==================================="
	@echo ""
	@echo "Infrastructure:"
	@echo "  make infra-up      - Start Docker Compose infrastructure"
	@echo "  make infra-down    - Stop Docker Compose infrastructure"
	@echo "  make infra-logs    - View infrastructure logs"
	@echo ""
	@echo "Build:"
	@echo "  make build         - Build all services with Maven"
	@echo "  make clean         - Clean all build artifacts"
	@echo ""
	@echo "Run Services (run each in separate terminal):"
	@echo "  make run-secrets   - Run secrets service (port 8081)"
	@echo "  make run-gateway   - Run gateway service (port 8080)"
	@echo "  make run-incident  - Run incident service (port 8082)"
	@echo "  make run-evidence  - Run evidence service (port 8083)"
	@echo "  make run-activity  - Run activity service (port 8084)"
	@echo "  make run-frontend  - Run frontend dev server (port 5173)"
	@echo ""
	@echo "Testing:"
	@echo "  make test-flow     - Run integration test flow script"
	@echo ""
	@echo "Quick Start:"
	@echo "  make start-all     - Start infra, build, and run all services"

# Infrastructure commands
infra-up:
	@echo "Starting PulseOps infrastructure..."
	docker-compose up -d
	@echo "Waiting for services to be healthy..."
	@sleep 5
	@docker-compose ps
	@echo ""
	@echo "Infrastructure started!"
	@echo "  - MongoDB: localhost:27017"
	@echo "  - Kafka: localhost:9092"
	@echo "  - MinIO API: localhost:9000"
	@echo "  - MinIO Console: http://localhost:9001"
	@echo "  - Jaeger UI: http://localhost:16686"

infra-down:
	@echo "Stopping PulseOps infrastructure..."
	docker-compose down
	@echo "Infrastructure stopped."

infra-logs:
	docker-compose logs -f

# Build commands
build:
	@echo "Building all PulseOps services..."
	mvn clean install -DskipTests
	@echo "Build complete!"

clean:
	@echo "Cleaning build artifacts..."
	mvn clean
	cd frontend && rm -rf node_modules dist
	@echo "Clean complete!"

# Service run commands
run-secrets:
	@echo "Starting Secrets Service on port 8081..."
	cd secrets-service && mvn spring-boot:run

run-gateway:
	@echo "Starting Gateway Service on port 8080..."
	cd gateway-service && mvn spring-boot:run

run-incident:
	@echo "Starting Incident Service on port 8082..."
	cd incident-service && mvn spring-boot:run

run-evidence:
	@echo "Starting Evidence Service on port 8083..."
	cd evidence-service && mvn spring-boot:run

run-activity:
	@echo "Starting Activity Service on port 8084..."
	cd activity-service && mvn spring-boot:run

run-frontend:
	@echo "Starting Frontend dev server on port 5173..."
	cd frontend && npm install && npm run dev

# Test commands
test-flow:
	@echo "Running integration test flow..."
	@chmod +x scripts/test-flow.sh
	@./scripts/test-flow.sh

# Quick start - start everything
start-all: infra-up build
	@echo ""
	@echo "=============================================="
	@echo "PulseOps v3.0 Quick Start"
	@echo "=============================================="
	@echo ""
	@echo "Infrastructure is running and services are built."
	@echo ""
	@echo "To start the services, run these commands in separate terminals:"
	@echo "  Terminal 1: make run-secrets"
	@echo "  Terminal 2: make run-gateway"
	@echo "  Terminal 3: make run-incident"
	@echo "  Terminal 4: make run-evidence"
	@echo "  Terminal 5: make run-activity"
	@echo "  Terminal 6: make run-frontend"
	@echo ""
	@echo "Once all services are running:"
	@echo "  - Frontend: http://localhost:5173"
	@echo "  - API Gateway: http://localhost:8080"
	@echo "  - Jaeger UI: http://localhost:16686"
	@echo ""
	@echo "Generate an API key via the Integrations page or with:"
	@echo "  curl -X POST http://localhost:8080/api/v1/keys \\"
	@echo "    -H 'Content-Type: application/json' \\"
	@echo "    -d '{\"name\":\"Test Key\"}'"
	@echo ""
