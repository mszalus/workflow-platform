# CLAUDE.md — Workflow Platform

## Current Plan

See [PLAN.md](PLAN.md) for the active implementation plan and progress tracker.

## Branches

- **`main`** — primary development branch, all active work lands here
- **`origin/fix/code-review-remediation`** — STALE, do not use. Contains an incompatible architectural rewrite (Maven, Kafka, completely different services). Open PR #1 should be closed.

## Project Overview

Multi-tenant BPMN workflow platform. Users design workflows visually (bpmn-js), deploy them, and end users complete tasks through a task inbox. Every action is audited, custom fields can be attached to any process, and notifications are delivered in real-time.

## Tech Stack

- **Java 21** / Spring Boot 3.3.5 / Spring Cloud 2023.0.3
- **Flowable 7.1.0** — BPMN engine with native tenant isolation (`TENANT_ID_` column)
- **PostgreSQL 16** — shared instance, one schema per service (workflow, custom_fields, notification, audit, keycloak)
- **RabbitMQ 3.13** — async events between services (topic exchange `wfp.events`)
- **Keycloak 25** — OIDC/JWT identity provider, single realm with Organizations for tenants
- **React 18 + TypeScript + Vite** — two frontend apps (admin-portal, user-portal)
- **Gradle 9.2 (Kotlin DSL)** — multi-module build with convention plugins in `buildSrc/`
- **npm workspaces** — frontend monorepo under `frontend/`
- **Docker Compose** — full local stack (10 containers)
- **Helm** — Kubernetes deployment (umbrella chart + per-service sub-charts)

## Project Structure

```
workflow-platform/
├── buildSrc/                    # Gradle convention plugins
│   └── src/main/kotlin/
│       ├── wfp.java-conventions.gradle.kts    # Java 21, UTF-8, JUnit 5
│       ├── wfp.library-conventions.gradle.kts # For shared libs (java-library + Lombok)
│       └── wfp.spring-boot-app.gradle.kts     # For services (Boot + Lombok + Spring Cloud BOM)
├── libs/                        # Shared libraries (not independently deployable)
│   ├── wfp-common/              # ErrorResponse, PagedResponse, GlobalExceptionHandler
│   ├── wfp-events/              # BaseEvent, EventConstants, all event types (polymorphic Jackson)
│   ├── wfp-security/            # SecurityConfig, TenantContext, TenantFilterAspect, JwtTenantConverter
│   └── wfp-test-support/        # JwtTestHelper, TenantTestHelper, TestContainersConfig
├── services/
│   ├── gateway/         (8080)  # Spring Cloud Gateway MVC, JWT validation, tenant header propagation
│   ├── workflow-service/ (8081) # Flowable engine, BPMN deploy/start/complete, event publishing
│   ├── custom-fields-service/ (8082)  # Dynamic field schemas + values per process definition
│   ├── notification-service/  (8083)  # RabbitMQ-driven notifications, mark-read, unread count
│   └── audit-service/         (8084)  # RabbitMQ-driven audit trail, queryable by entity/user/time
├── frontend/
│   ├── packages/
│   │   ├── shared-ui/           # Shared React components, API client, auth provider, types
│   │   └── bpmn-editor/         # bpmn-js wrapper component
│   └── apps/
│       ├── admin-portal/        # Process designer, deployment, custom field editor, audit log
│       └── user-portal/         # Task inbox, start process, notifications, dynamic forms
├── docker/
│   ├── docker-compose.yml       # Full stack (PG, RabbitMQ, Keycloak, 5 services, 2 frontends)
│   ├── init-db.sql              # Creates per-service schemas
│   └── keycloak/realm-export.json
└── helm/
    ├── charts/                  # Per-service Helm sub-charts
    └── workflow-platform/       # Umbrella chart (Chart.yaml, values-local.yaml)
```

## Build Commands

### Backend (requires JDK 21)
```bash
./gradlew build                                    # compile + test all modules
./gradlew :services:workflow-service:bootJar -x test  # build single service JAR
./gradlew :services:workflow-service:test           # test single service
```

### Frontend
```bash
cd frontend
npm ci                                              # install deps (clean)
npm run build                                       # build all packages + apps (order matters)
npm run typecheck --workspaces --if-present          # TypeScript type check
npm run dev:admin                                   # dev server for admin-portal
npm run dev:user                                    # dev server for user-portal
```

### Docker
```bash
docker compose -f docker/docker-compose.yml build              # build all images
docker compose -f docker/docker-compose.yml up -d              # start full stack
docker compose -f docker/docker-compose.yml logs <service>     # view logs
docker compose -f docker/docker-compose.yml config --quiet     # validate compose file
```

### Helm
```bash
helm lint helm/charts/<chart-name>/                            # lint single chart
helm dependency update helm/workflow-platform/                 # pull bitnami deps
helm install wfp helm/workflow-platform/ -f helm/workflow-platform/values-local.yaml
```

## Architecture Patterns

### Multi-Tenancy
Every request carries a tenant ID extracted from the JWT `tenant_id` claim.
- **Gateway** → `TenantHeaderFilter` adds `X-Tenant-Id` header to downstream requests
- **Services** → `TenantInterceptor` reads the header and sets `TenantContext` (ThreadLocal)
- **JPA** → Hibernate `@FilterDef`/`@Filter` on entities auto-filters by `tenant_id`
- **Flowable** → All engine calls include `tenantId` parameter
- **CRITICAL**: Only ONE `@FilterDef(name = "tenantFilter")` per persistence unit. Additional entities in the same service must use `@Filter` only (no `@FilterDef`).

### Event System (RabbitMQ)
- Topic exchange: `wfp.events`
- Routing keys: `task.created`, `task.assigned`, `task.completed`, `process.started`, `process.completed`, etc.
- Queues: `wfp.notification` (binds `task.*` + `process.completed`), `wfp.audit` (binds `#` = all)
- Events use Jackson polymorphic serialization (`@JsonTypeInfo` on `BaseEvent`)
- All event types defined in `libs/wfp-events/`

### Gateway Routing
Gateway rewrites paths to match backend service endpoints:
- `/api/workflow/**` → workflow-service `/api/**` (via `RewritePath`)
- `/api/fields/**` → custom-fields-service `/api/**` (via `RewritePath`)
- `/api/notifications/**` → notification-service `/api/notifications/**` (pass-through)
- `/api/audit/**` → audit-service `/api/audit/**` (pass-through)

In Docker, URIs are overridden via env vars (`SPRING_CLOUD_GATEWAY_MVC_ROUTES_N_URI`).

### Security
- All services use OAuth2 resource server with JWT validation against Keycloak
- Gateway excludes `DataSourceAutoConfiguration` and `HibernateJpaAutoConfiguration` (it has no database)
- Public endpoints: `/actuator/health`, `/actuator/info`, `/v3/api-docs/**`, `/swagger-ui/**`

## Known Pitfalls

1. **Gradle requires all project directories**: `settings.gradle.kts` includes all modules — Dockerfiles must copy the entire `services/` directory, not just the target service
2. **Port conflicts**: Local PostgreSQL on 5432 conflicts with Docker. Docker compose maps PG to `5433` externally
3. **Hibernate @FilterDef**: Only one per persistence unit, not per entity. Second entity → use `@Filter` only
4. **RabbitMQ Jackson**: Messages need `Jackson2JsonMessageConverter` bean in the RabbitMQ config
5. **Flowable + H2 tests**: Requires `MODE=LEGACY` in the JDBC URL, not `MODE=PostgreSQL`
6. **EventPublisher**: Inject `@Nullable RabbitTemplate` — test contexts may not have RabbitMQ
7. **Frontend build order**: `shared-ui` → `bpmn-editor` → apps (apps depend on packages)
8. **CI gradlew permission**: The `gradlew` file must have execute permission in git (`git update-index --chmod=+x gradlew`)

## CI Pipeline (.github/workflows/ci.yml)

Runs on push to `main` and on PRs targeting `main`. Four parallel jobs:
1. **backend-build** — `./gradlew build` with JDK 21
2. **frontend-build** — `npm ci` + `npm run typecheck` with Node 20
3. **docker-build** — validates docker-compose (only after 1+2 pass, only on main)
4. **helm-lint** — `helm lint` on each sub-chart

## Testing

- Backend integration tests use **Testcontainers** (PostgreSQL + RabbitMQ)
- Test config: `src/test/resources/application-test.yml` with `SPRING_PROFILES_ACTIVE=test`
- `JwtTestHelper` generates mock JWTs for authenticated endpoint tests
- `TenantTestHelper` sets up `TenantContext` for service-layer tests
- Frontend: TypeScript typecheck only (no unit test framework yet)

## MCP Servers

- **Playwright** (`@playwright/mcp`) — browser automation for E2E testing. Use for verifying Keycloak, RabbitMQ management UI, frontend portals, and gateway health endpoints.

## Working Agreements

- **Verify before claiming done**: Build, run tests, and start the application if infra is available. Don't commit untested code.
- **Don't push broken CI**: Check that `./gradlew build` and `npm run typecheck` pass before pushing to `main`.
- **Commit granularity**: Logical commits — one per feature/fix, not one per file.
