# Workflow Platform

A multi-tenant BPMN workflow platform built with Flowable, Spring Boot microservices, and React. Users design workflows visually, deploy them, and end users complete tasks through a web-based task inbox. Every action is audited, custom fields can be attached to any process definition, and notifications are delivered in real-time via RabbitMQ.

## Architecture

```
                         ┌──────────────┐
                         │   Keycloak   │
                         │   (OIDC/JWT) │
                         └──────┬───────┘
                                │
        ┌───────────────────────┼───────────────────────┐
        │                  API Gateway (:8080)           │
        │          JWT validation + tenant routing       │
        └──┬──────────┬──────────┬──────────┬───────────┘
           │          │          │          │
     ┌─────┴──┐ ┌─────┴──┐ ┌────┴───┐ ┌───┴────┐
     │Workflow│ │Custom  │ │Notif.  │ │ Audit  │
     │Service │ │Fields  │ │Service │ │Service │
     │ :8081  │ │Service │ │ :8083  │ │ :8084  │
     │        │ │ :8082  │ │        │ │        │
     └──┬──┬──┘ └──┬─────┘ └──┬─────┘ └──┬────┘
        │  │       │          │           │
        │  │  ┌────┴──────────┴───────────┘
        │  │  │         RabbitMQ
        │  │  │      (async events)
        │  │  └─────────────────────────────
        │  │
     ┌──┴──┴──┐
     │PostgreSQL│  ← schema-per-service
     │  :5432   │    (workflow, custom_fields,
     └─────────┘     notification, audit)

        ┌─────────────┐    ┌─────────────┐
        │Admin Portal │    │ User Portal │
        │   :5173     │    │   :5174     │
        └─────────────┘    └─────────────┘
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| BPMN Engine | Flowable 7.1.0 |
| Backend | Java 21, Spring Boot 3.3.5, Spring Cloud 2023.0.3 |
| API Gateway | Spring Cloud Gateway MVC |
| Database | PostgreSQL 16 (schema-per-service) |
| Messaging | RabbitMQ 3.13 (topic exchange) |
| Identity | Keycloak 25 (OIDC/JWT, Organizations for tenants) |
| Frontend | React 18, TypeScript, Vite, bpmn-js |
| Build | Gradle 9.2 (Kotlin DSL), npm workspaces |
| Deployment | Docker Compose, Helm/Kubernetes |

## Prerequisites

- Docker & Docker Compose (for quick start)
- JDK 21+ (for local backend development)
- Node.js 20+ (for local frontend development)

## Quick Start (Docker Compose)

```bash
# Clone the repository
git clone https://github.com/mszalus/workflow-platform.git
cd workflow-platform

# Build and start all services
docker compose -f docker/docker-compose.yml build
docker compose -f docker/docker-compose.yml up -d

# Check all containers are running
docker compose -f docker/docker-compose.yml ps
```

Once running, the services are available at:

| Service | URL |
|---------|-----|
| API Gateway | http://localhost:9080 |
| Keycloak Admin | http://localhost:8180 (admin/admin) |
| RabbitMQ Management | http://localhost:15672 (wfp/wfp_secret) |
| Admin Portal | http://localhost:5173 |
| User Portal | http://localhost:5174 |
| Workflow Service (direct) | http://localhost:8081 |
| Custom Fields Service (direct) | http://localhost:8082 |
| Notification Service (direct) | http://localhost:8083 |
| Audit Service (direct) | http://localhost:8084 |
| PostgreSQL | localhost:5433 (wfp/wfp_secret) |

## Local Development

### Backend

```bash
# Build all modules
./gradlew build

# Build a single service JAR (skip tests)
./gradlew :services:workflow-service:bootJar -x test

# Run tests for a single service
./gradlew :services:audit-service:test

# Start a service locally (requires PG, RabbitMQ, Keycloak running)
./gradlew :services:workflow-service:bootRun
```

### Frontend

```bash
cd frontend

# Install dependencies
npm ci

# Start admin portal dev server
npm run dev:admin

# Start user portal dev server
npm run dev:user

# Build all packages and apps
npm run build

# TypeScript type check
npm run typecheck --workspaces --if-present
```

### Infrastructure Only

To run just the backing services (for local development of backend/frontend):

```bash
docker compose -f docker/docker-compose.yml up -d postgres rabbitmq keycloak
```

## API Endpoints

All endpoints require a valid JWT from Keycloak (except health checks).

### Via Gateway (http://localhost:9080)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/workflow/deployments` | Deploy a BPMN process |
| GET | `/api/workflow/deployments` | List deployments |
| POST | `/api/workflow/processes` | Start a process instance |
| GET | `/api/workflow/processes` | List process instances |
| GET | `/api/workflow/tasks` | List tasks |
| POST | `/api/workflow/tasks/{id}/complete` | Complete a task |
| GET | `/api/workflow/history/processes` | Process history |
| POST | `/api/fields/schemas` | Create a custom field schema |
| GET | `/api/fields/schemas?processDefinitionKey=...` | List field schemas |
| POST | `/api/fields/values` | Save field values |
| GET | `/api/fields/values?processInstanceId=...` | Get field values |
| GET | `/api/notifications/` | List notifications |
| GET | `/api/notifications/unread-count` | Get unread count |
| PUT | `/api/notifications/mark-read` | Mark notifications as read |
| GET | `/api/audit/` | Query audit trail |

## Project Structure

```
workflow-platform/
├── buildSrc/                    # Gradle convention plugins (Java 21, Spring Boot, Lombok)
├── libs/                        # Shared libraries
│   ├── wfp-common/              # DTOs, exception handling
│   ├── wfp-events/              # RabbitMQ event types (polymorphic Jackson)
│   ├── wfp-security/            # JWT auth, tenant context, Hibernate tenant filter
│   └── wfp-test-support/        # Test helpers (JWT mocking, Testcontainers)
├── services/
│   ├── gateway/                 # API Gateway (routing, JWT validation, tenant headers)
│   ├── workflow-service/        # Flowable BPMN engine + REST API
│   ├── custom-fields-service/   # Dynamic field schemas and values
│   ├── notification-service/    # Event-driven notifications
│   └── audit-service/           # Event-driven audit trail
├── frontend/
│   ├── packages/shared-ui/      # Shared components, API client, auth
│   ├── packages/bpmn-editor/    # bpmn-js wrapper
│   ├── apps/admin-portal/       # Process design + admin UI
│   └── apps/user-portal/        # Task inbox + user UI
├── docker/                      # Docker Compose + Keycloak realm config
└── helm/                        # Kubernetes Helm charts
```

## Multi-Tenancy

Tenant isolation is enforced at every layer:

1. **JWT** — Keycloak issues tokens with a `tenant_id` claim
2. **Gateway** — extracts tenant from JWT, adds `X-Tenant-Id` header to downstream requests
3. **Services** — `TenantInterceptor` stores tenant in `TenantContext` (ThreadLocal)
4. **JPA** — Hibernate `@Filter` automatically adds `WHERE tenant_id = :tenantId` to all queries
5. **Flowable** — all engine API calls include `tenantId`

## Testing

```bash
# Run all backend tests (uses Testcontainers — requires Docker)
./gradlew build

# Run frontend type checks
cd frontend && npm run typecheck --workspaces --if-present
```

Backend integration tests use Testcontainers to spin up PostgreSQL and RabbitMQ automatically. No manual infrastructure setup needed.

## CI/CD

GitHub Actions CI runs on every push to `main` and on pull requests:

1. **backend-build** — Gradle build + test (JDK 21)
2. **frontend-build** — npm install + TypeScript typecheck
3. **docker-build** — validates docker-compose config (only on main, after 1+2 pass)
4. **helm-lint** — lints all Helm sub-charts

## Kubernetes Deployment

```bash
# Update Helm dependencies (pulls Bitnami charts for PG, RabbitMQ, Keycloak)
helm dependency update helm/workflow-platform/

# Install with local values
helm install wfp helm/workflow-platform/ -f helm/workflow-platform/values-local.yaml

# Lint charts
for chart in helm/charts/*/; do helm lint "$chart"; done
```
