# PulseOps v3.0

A modern incident management platform built with microservices architecture, featuring real-time activity feeds, evidence management, and distributed tracing.

## Architecture Overview

PulseOps v3.0 is built on a microservices architecture with the following components:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              PULSEOPS v3.0                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌──────────────┐                                                           │
│  │   Frontend   │  React 18 + Vite + Tailwind CSS                          │
│  │   (Port 80)  │  Single Page Application                                 │
│  └──────┬───────┘                                                           │
│         │                                                                   │
│         ▼                                                                   │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    API Gateway (Port 8080)                          │   │
│  │         Spring Cloud Gateway - Single Entry Point                   │   │
│  │              API Key Validation, Correlation ID                     │   │
│  └──────┬────────────┬────────────┬────────────┬───────────────────────┘   │
│         │            │            │            │                            │
│         ▼            ▼            ▼            ▼                            │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐                    │
│  │ Secrets  │  │ Incident │  │ Evidence │  │ Activity │                    │
│  │ Service  │  │ Service  │  │ Service  │  │ Service  │                    │
│  │(:8081)   │  │(:8082)   │  │(:8083)   │  │(:8084)   │                    │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘                    │
│       │              │            │            │                            │
│       └──────────────┴────────────┴────────────┘                            │
│                      │                                                      │
│  ┌───────────────────┼───────────────────────────────────────────────┐     │
│  │                   ▼                                               │     │
│  │  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐  │     │
│  │  │ MongoDB │  │  Kafka  │  │  MinIO  │  │  Jaeger │  │  OTel   │  │     │
│  │  │(:27017) │  │(:9092)  │  │(:9000)  │  │(:16686) │  │(:4318)  │  │     │
│  │  └─────────┘  └─────────┘  └─────────┘  └─────────┘  └─────────┘  │     │
│  │                                                                   │     │
│  └──────────────────────── Infrastructure Layer ─────────────────────┘     │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Service Descriptions

| Service | Port | Description | Database | Events |
|---------|------|-------------|----------|--------|
| **Gateway** | 8080 | API Gateway, routing, API key validation | - | - |
| **Secrets** | 8081 | API key generation and validation | MongoDB | - |
| **Incident** | 8082 | Incident CRUD, stale detection | MongoDB | Publishes |
| **Evidence** | 8083 | File upload/download with MinIO | MongoDB + MinIO | Publishes |
| **Activity** | 8084 | Activity feed, real-time SSE | MongoDB | Consumes |

### Event Flow

```
Incident Service ──┐
                   ├──► Kafka (pulseops.domain-events) ──► Activity Service
Evidence Service ──┘                                          │
                                                              ▼
                                                    Real-time SSE to Frontend
```

## Prerequisites

- **Docker** 24.0+ and Docker Compose
- **Java** 21 (OpenJDK or Temurin)
- **Maven** 3.9+
- **Node.js** 18+ and npm
- **curl** (for testing)

## Quick Start

### 1. Start Infrastructure

Start all infrastructure services (MongoDB, Kafka, MinIO, Jaeger, etc.):

```bash
docker-compose up -d
```

Wait for all services to be healthy (about 30-60 seconds):

```bash
docker-compose ps
```

### 2. Build All Services

```bash
mvn clean install
```

### 3. Start Services (in order)

**Terminal 1 - Secrets Service:**
```bash
cd secrets-service && mvn spring-boot:run
```

**Terminal 2 - Gateway Service:**
```bash
cd gateway-service && mvn spring-boot:run
```

**Terminal 3 - Incident Service:**
```bash
cd incident-service && mvn spring-boot:run
```

**Terminal 4 - Evidence Service:**
```bash
cd evidence-service && mvn spring-boot:run
```

**Terminal 5 - Activity Service:**
```bash
cd activity-service && mvn spring-boot:run
```

### 4. Start Frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend will be available at: http://localhost:5173

## Generate API Key

### Via UI

1. Open http://localhost:5173
2. Navigate to "Integrations" page
3. Click "Generate New API Key"
4. Copy the displayed key (shown only once!)

### Via curl

```bash
curl -X POST http://localhost:8080/api/v1/keys \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Key",
    "description": "For testing"
  }'
```

Response:
```json
{
  "keyId": "key_abc123",
  "apiKey": "pulseops_live_xxxxxxxxxxxx",
  "name": "Test Key",
  "createdAt": "2024-01-15T10:30:00Z"
}
```

**⚠️ Save the `apiKey` value - it will not be shown again!**

## Access Points

| Service | URL | Credentials |
|---------|-----|-------------|
| **Frontend** | http://localhost:5173 | - |
| **API Gateway** | http://localhost:8080 | API Key required |
| **Jaeger UI** | http://localhost:16686 | - |
| **MinIO Console** | http://localhost:9001 | pulseops / pulseops_secret_key |
| **MinIO API** | http://localhost:9000 | pulseops / pulseops_secret_key |

## API Endpoints

All endpoints (except key generation) require the header:
```
X-API-Key: your_api_key_here
```

### Secrets Service
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/keys` | Create API key |
| POST | `/api/v1/keys/verify` | Verify API key |
| DELETE | `/api/v1/keys/{id}` | Revoke API key |

### Incident Service
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/incidents` | Create incident |
| GET | `/api/v1/incidents` | List incidents |
| GET | `/api/v1/incidents/{id}` | Get incident |
| PUT | `/api/v1/incidents/{id}` | Update incident |
| DELETE | `/api/v1/incidents/{id}` | Delete incident |
| POST | `/api/v1/incidents/{id}/status` | Change status |
| POST | `/api/v1/incidents/{id}/assign` | Assign incident |

### Evidence Service
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/evidence` | Upload file |
| GET | `/api/v1/evidence/incident/{id}` | List evidence |
| GET | `/api/v1/evidence/{id}/download` | Download file |
| DELETE | `/api/v1/evidence/{id}` | Delete evidence |

### Activity Service
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/activity` | Get activity feed |
| GET | `/api/v1/activity/stream` | SSE real-time stream |

## Testing the Acceptance Criteria

### 1. Create API Key

```bash
API_KEY_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/keys \
  -H "Content-Type: application/json" \
  -d '{"name": "Test Key", "description": "Testing"}')
API_KEY=$(echo $API_KEY_RESPONSE | grep -o '"apiKey":"[^"]*"' | cut -d'"' -f4)
echo "API Key: $API_KEY"
```

### 2. Create Incident

```bash
curl -X POST http://localhost:8080/api/v1/incidents \
  -H "X-API-Key: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Database Connection Failure",
    "description": "Production database is not responding",
    "severity": "critical",
    "service": "user-service"
  }'
```

### 3. Update Incident Status

```bash
curl -X POST "http://localhost:8080/api/v1/incidents/{incident-id}/status?newStatus=investigating" \
  -H "X-API-Key: $API_KEY"
```

### 4. Upload Evidence

```bash
curl -X POST http://localhost:8080/api/v1/evidence \
  -H "X-API-Key: $API_KEY" \
  -F "incidentId={incident-id}" \
  -F "description=Error screenshot" \
  -F "file=@/path/to/screenshot.png"
```

### 5. Query Activity

```bash
curl "http://localhost:8080/api/v1/activity?incidentId={incident-id}" \
  -H "X-API-Key: $API_KEY"
```

### 6. Verify SSE Connection

```bash
curl -N "http://localhost:8080/api/v1/activity/stream" \
  -H "X-API-Key: $API_KEY" \
  -H "Accept: text/event-stream"
```

## Development

### Project Structure

```
pulseops-v3/
├── pom.xml                    # Parent Maven POM
├── docker-compose.yml         # Infrastructure services
├── Makefile                   # Convenience commands
├── README.md                  # This file
├── secrets-service/           # API Key management
├── gateway-service/           # API Gateway
├── incident-service/          # Incident management
├── evidence-service/          # File storage
├── activity-service/          # Activity feed & SSE
└── frontend/                  # React SPA
```

### Running Tests

```bash
# Run all tests
mvn test

# Run tests for specific service
cd incident-service && mvn test
```

### Building for Production

```bash
# Build all services
mvn clean package

# Build frontend
cd frontend && npm run build
```

### Viewing Traces

1. Open Jaeger UI: http://localhost:16686
2. Select `pulseops-gateway` or any service from the Service dropdown
3. Click "Find Traces" to see distributed traces

## Troubleshooting

### Services fail to start

Check if infrastructure is running:
```bash
docker-compose ps
```

All services should show `healthy` status.

### Kafka connection issues

Wait for Kafka to be fully initialized:
```bash
docker logs pulseops-kafka
```

### MongoDB connection refused

Ensure MongoDB is healthy:
```bash
docker logs pulseops-mongodb
```

### API Key rejected

Verify the key is valid:
```bash
curl -X POST http://localhost:8080/api/v1/keys/verify \
  -H "Content-Type: application/json" \
  -d '{"apiKey": "your_key_here"}'
```

## Technology Stack

| Layer | Technology |
|-------|------------|
| **Frontend** | React 18, Vite, Tailwind CSS |
| **Gateway** | Spring Cloud Gateway |
| **Services** | Spring Boot 3.2, Java 21 |
| **Data** | MongoDB 7, MinIO (S3-compatible) |
| **Messaging** | Apache Kafka |
| **Observability** | OpenTelemetry, Jaeger |
| **Build** | Maven, npm |

## License

MIT License - See LICENSE file for details.

## Support

For issues or questions, please open an issue in the project repository.
