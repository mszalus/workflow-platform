# Workflow Platform

A multi-tenant BPMN workflow platform built with Flowable, Spring Boot and React. Users design workflows visually, deploy them, and end users complete tasks through a web-based task inbox. Every action is audited, custom fields can be attached to any process definition, and in-app notifications are written in the same transaction as the change that causes them.

## Architecture

```
   ┌─────────────┐   ┌─────────────┐        ┌──────────────┐
   │Admin Portal │   │ User Portal │        │   Keycloak   │
   │   :5173     │   │   :5174     │        │  (OIDC/JWT)  │
   └──────┬──────┘   └──────┬──────┘        └──────────────┘
          │ nginx /api proxy│
          └────────┬────────┘
                   │
       ┌───────────┴───────────┐
       │   App                 │
       │        :8081          │
       │ + custom fields,      │
       │   notifications,      │
       │   audit               │
       └───────────┬───────────┘
                   │
            ┌──────┴──────┐
            │ PostgreSQL  │
            │   :5432     │
            │  (workflow) │
            └─────────────┘
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| BPMN Engine | Flowable 7.1.0 |
| Backend | Java 21, Spring Boot 3.5 |
| Database | PostgreSQL 16 |
| Identity | Keycloak 25 (OIDC/JWT, tenant as a user attribute) |
| Frontend | React 18, TypeScript, Vite, bpmn-js |
| Build | Gradle 9.2 (Groovy DSL), npm workspaces |
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
| Keycloak Admin | http://localhost:8180 (admin/admin) |
| Admin Portal | http://localhost:5173 |
| User Portal | http://localhost:5174 |
| App (API) | http://localhost:8081 |
| PostgreSQL | localhost:5433 (wfp/wfp_secret) |

## Local Development

### Backend

```bash
# Build all modules
./gradlew build

# Build a single service JAR (skip tests)
./gradlew :services:app:bootJar -x test

# Run tests for a single service
./gradlew :services:app:test

# Start a service locally (requires PG and Keycloak running)
./gradlew :services:app:bootRun
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
docker compose -f docker/docker-compose.yml up -d postgres keycloak
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
├── services/
│   └── app/                     # Flowable BPMN engine, custom fields, notifications, audit, REST API
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
2. **Service** — `TenantInterceptor` reads `tenant_id` from the validated JWT into `TenantContext` (ThreadLocal) and rejects a token without it (403)
3. **JPA** — Hibernate `@Filter` automatically adds `WHERE tenant_id = :tenantId` to all queries
4. **Flowable** — all engine API calls include `tenantId`

## Testing

```bash
# Run all backend tests (H2, no Docker needed)
./gradlew build

# Run frontend type checks
cd frontend && npm run typecheck --workspaces --if-present
```

Backend integration tests run the full Spring context against an in-memory H2 database. No infrastructure setup needed.

## CI/CD

GitHub Actions CI runs on every push to `main` and on pull requests:

1. **backend-build** — Gradle build + test (JDK 21)
2. **frontend-build** — npm install + TypeScript typecheck
3. **docker-build** — validates docker-compose config (only on main, after 1+2 pass)
4. **helm-lint** — lints all Helm sub-charts

## Kubernetes Deployment

```bash
# Update Helm dependencies (pulls Bitnami charts for PG and Keycloak)
helm dependency update helm/workflow-platform/

# Install with local values
helm install wfp helm/workflow-platform/ -f helm/workflow-platform/values-local.yaml

# Lint charts
for chart in helm/charts/*/; do helm lint "$chart"; done
```
