# Workflow Platform

A multi-tenant BPMN workflow platform built with Flowable, Spring Boot and React. Users design workflows visually, deploy them, and teams track work items through them in a web app. Every action is audited, custom fields can be defined per project and item type, and in-app notifications are written in the same transaction as the change that causes them.

## Architecture

```
   ┌─────────────┐                        ┌──────────────┐
   │ Tracker app │                        │   Keycloak   │
   │    :5175    │                        │  (OIDC/JWT)  │
   └──────┬──────┘                        └──────────────┘
          │ nginx /api proxy
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
| BPMN Engine | Flowable 8.0.0 |
| Backend | Java 21, Spring Boot 4.0 |
| Database | PostgreSQL 16 |
| Identity | Keycloak 26 (OIDC/JWT, tenant as an Organization) |
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
| Tracker app | http://localhost:5175 |
| App (API) | http://localhost:8081 |
| PostgreSQL | localhost:5433 (wfp/wfp_secret) |

A fresh stack has no projects. To add a demo project (`DEMO`) with a custom field and a few items in `tenant-a`:

```bash
cd e2e && npm ci && npm run demo-data
```

Running it again changes nothing.

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

# Start the dev server
npm run dev:web

# Build the app
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
| POST | `/api/projects` | Create a project with its item types |
| GET | `/api/projects` | List projects |
| PUT | `/api/projects/{key}` | Rename a project and add or remove item types |
| POST | `/api/items` | Create a work item |
| GET | `/api/items?project=...&assignee=...` | List work items |
| GET | `/api/items/{key}` | Get a work item with its available transitions |
| PATCH | `/api/items/{key}` | Change fixed or custom fields |
| POST | `/api/items/{key}/transitions` | Move a work item to another status |
| GET | `/api/items/{key}/history` | Status and field changes, newest first |
| GET | `/api/items/{key}/attachments` | List a work item's files, newest first |
| POST | `/api/items/{key}/attachments` | Upload a file, up to 10 MB (multipart field `file`) |
| GET | `/api/items/{key}/attachments/{id}/content` | Download a file |
| DELETE | `/api/items/{key}/attachments/{id}` | Delete a file (its uploader or an admin) |
| GET | `/api/items/{key}/comments` | List a work item's comments, oldest first |
| POST | `/api/items/{key}/comments` | Comment on a work item and notify its reporter and assignee |
| POST | `/api/workflow/deployments` | Deploy a BPMN process (tracker workflows are validated first) |
| POST | `/api/workflow/workflows/upgrade-preview` | Dry run: how moving open items to this BPMN would affect them |
| POST | `/api/workflow/workflows/{key}/upgrade` | Move open items to the latest version |
| POST | `/api/workflow/workflows/validate` | Check BPMN against the tracker profile: `[{elementId, rule, message}]` |
| GET | `/api/workflow/deployments` | List the tenant's workflows and the built-in `default` one |
| POST | `/api/fields/schemas` | Create a custom field schema for a `project` and `itemTypes` (none means all) |
| GET | `/api/fields/schemas?project=...&type=...` | List a project's field schemas, optionally those that apply to one item type |
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
│   └── apps/web/                # Tracker app: user and admin screens, BPMN editor
├── docker/                      # Docker Compose + Keycloak realm config
└── helm/                        # Kubernetes Helm charts
```

## Multi-Tenancy

Tenant isolation is enforced at every layer:

1. **JWT** — Keycloak issues tokens with an `organization` claim listing the user's Keycloak Organization, whose alias is the tenant id
2. **Service** — `TenantInterceptor` reads the organization from the validated JWT into `TenantContext` (ThreadLocal) and rejects a token with no organization or several (403)
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
