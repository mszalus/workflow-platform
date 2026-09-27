# CLAUDE.md — Workflow Platform

## Current Plan

See [PLAN.md](PLAN.md) for the active implementation plan and progress tracker.

## Engineering Process

Sessions, branches and merging, gates, review and subagent delegation are described in [docs/engineering-process.md](docs/engineering-process.md). In short:

- **`main`** is protected and the only long-lived branch. Work lands through short-lived branches and PRs that pass `backend-build`, `frontend-build` and `helm-lint`. Only the human merges.
- Delegate mechanical work to the subagents in `.claude/agents/` (`test-runner`, `vault-rebuilder`, `ci-triager`). Keep design and tenancy or security changes in the main session.
- The old `master` and `fix/code-review-remediation` branches (the incompatible Maven/Kafka design) were deleted on 2026-09-26.

## Project Overview

Multi-tenant BPMN workflow platform. Users design workflows visually (bpmn-js), deploy them, and end users complete tasks through a task inbox. Every action is audited, custom fields can be attached to any process, and notifications are delivered in real-time.

## Tech Stack

- **Java 21** / Spring Boot 3.3.5 / Spring Cloud 2023.0.3
- **Flowable 7.1.0** — BPMN engine with native tenant isolation (`TENANT_ID_` column)
- **PostgreSQL 16** — shared instance, one schema per service (workflow, custom_fields, notification, audit, keycloak)
- **RabbitMQ 3.13** — async events between services (topic exchange `wfp.events`)
- **Keycloak 25** — OIDC/JWT identity provider, single realm with Organizations for tenants
- **React 18 + TypeScript + Vite** — two frontend apps (admin-portal, user-portal)
- **Gradle 9.2 (Groovy DSL)** — multi-module build with convention plugins in `buildSrc/`
- **npm workspaces** — frontend monorepo under `frontend/`
- **Docker Compose** — full local stack (10 containers)
- **Helm** — Kubernetes deployment (umbrella chart + per-service sub-charts)

## Project Structure

```
workflow-platform/
├── buildSrc/                    # Gradle convention plugins
│   └── src/main/groovy/
│       ├── wfp.java-conventions.gradle    # Java 21, UTF-8, JUnit 5
│       ├── wfp.library-conventions.gradle # For shared libs (java-library + Lombok)
│       └── wfp.spring-boot-app.gradle     # For services (Boot + Lombok + Spring Cloud BOM)
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
├── docs/
│   ├── admin-manual.md          # Source doc — hand-edited
│   ├── user-manual.md           # Source doc — hand-edited
│   ├── architecture/            # C4 diagrams + ERD (Mermaid) — hand-edited
│   ├── screenshots/             # PNGs captured from the running stack
│   └── vault/                   # GENERATED Obsidian vault — never hand-edit
├── tools/
│   └── vault-build/             # Generator for docs/vault (run_all.sh)
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

1. **Gradle requires all project directories**: `settings.gradle` includes all modules — Dockerfiles must copy the entire `services/` directory, not just the target service
2. **Port conflicts**: Local PostgreSQL on 5432 conflicts with Docker. Docker compose maps PG to `5433` externally
3. **Hibernate @FilterDef**: Only one per persistence unit, not per entity. Second entity → use `@Filter` only
4. **RabbitMQ Jackson**: Messages need `Jackson2JsonMessageConverter` bean in the RabbitMQ config
5. **Flowable + H2 tests**: Requires `MODE=LEGACY` in the JDBC URL, not `MODE=PostgreSQL`
6. **EventPublisher**: Inject `@Nullable RabbitTemplate` — test contexts may not have RabbitMQ
7. **Frontend build order**: `shared-ui` → `bpmn-editor` → apps (apps depend on packages)
8. **CI gradlew permission**: The `gradlew` file must have execute permission in git (`git update-index --chmod=+x gradlew`)
9. **Gradle daemon JDK**: Gradle 9.2 cannot run on JDK 26+. `gradle/gradle-daemon-jvm.properties` pins the daemon to Java 21, which Gradle picks from locally installed JDKs whatever `JAVA_HOME` says

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

## Documentation Vault (`docs/vault/`)

An Obsidian vault — ~140 notes, fully cross-linked — that indexes this codebase. **Read it
before exploring the source tree**: it is usually faster than grepping, and it records the
*why* behind decisions that the code alone does not explain.

### Where to look

| Question | Note |
|----------|------|
| What is this system? | `Home.md`, then `00-Index/*.md` (MOC hub notes) |
| How do the pieces fit? | `10-Architecture/` — C4 L1→L4 + ERD, all Mermaid |
| What does service X do? | `20-Services/<Service Name>.md` |
| Why is it built this way? | `30-Concepts/` — tenancy, events, security, pitfalls |
| Which endpoint / event / table? | `35-Reference/` — generated from source |
| How do I run or deploy it? | `40-Operations/` |
| How does a user do X? | `50-Manuals/` |
| What is left to do? | `60-Project/` — split from PLAN.md, `status:` in frontmatter |

Every note carries `source:` frontmatter naming the files it was derived from.

### Rules

- **Never hand-edit anything under `docs/vault/`.** It is build output; the whole tree is
  emptied and regenerated on every run. Edit the source doc (`docs/*.md`, `PLAN.md`,
  `README.md`) or the generator in `tools/vault-build/`, then rebuild.
- **Regenerate after changing controllers, entities, or event types** — the notes in
  `35-Reference/` and `20-Services/` are derived from those and will otherwise drift:
  ```bash
  bash tools/vault-build/run_all.sh     # rebuilds, then validates every wikilink
  ```
  A clean build reports `broken: 0`, `ORPHANS: 0`, `DEAD ENDS: 0`. Treat anything else as
  a failure.
- Adding a controller, entity, or event means updating the corresponding list in
  `content_reference.py` or `content_services.py` — the generator does not auto-discover.

### Open gaps the vault records

`Attachment` has an entity and repository but no REST endpoint; `NotificationPreference`
is never consulted before creating a notification; `process.sla.breached`,
`field.schema.created` and `field.value.saved` are declared in `EventConstants` with no
publisher. See `60-Project/Project MOC.md`.

## MCP Servers

- **Playwright** (`@playwright/mcp`) — browser automation for E2E testing. Use for verifying Keycloak, RabbitMQ management UI, frontend portals, and gateway health endpoints.

## Plugins & Skills

- **`obsidian@obsidian-skills`** (third-party, MIT, `kepano/obsidian-skills`) — Obsidian
  Flavored Markdown, Bases, and JSON Canvas skills. Use `obsidian-markdown` when editing
  the generator's note templates so wikilinks, embeds, callouts and properties stay valid.
- **`claude-code-setup@claude-plugins-official`** (Anthropic) — recommends Claude Code
  automations for this repo. Note it does not know about the hooks and permissions already
  configured in `.claude/settings.json`, so expect overlap in its suggestions.

## Working Agreements

- **Verify before claiming done**: Build, run tests, and start the application if infra is available. Don't commit untested code.
- **Don't push broken CI**: Check that `./gradlew build` and `npm run typecheck` pass before pushing to `main`.
- **Commit granularity**: Logical commits — one per feature/fix, not one per file.
- **Minimal comments**: Write as few comments as possible. Express intent through method and variable names, and extract helpers instead of writing explanatory comments. Only comment when something genuinely cannot be expressed through naming (e.g., a non-obvious external library workaround).
- **Simple over clever**: Build the simplest design that meets today's need. Avoid speculative abstractions, extra layers, configurability nobody asked for, and a second implementation of an interface "just in case". When proposing a design, say which parts could be cut and default to cutting them.
- **Standards before custom builds**: Prefer an existing standard or tool the stack already has (BPMN plus the bpmn-js editor, Flowable, Keycloak, PostgreSQL `jsonb`) over writing our own engine, DSL or framework.
- **Thin facades only at replaceable boundaries**: Put a third-party engine that may be swapped (Flowable → Camunda 7, Operaton, Activiti) behind a small interface written in domain terms, with one adapter. Don't wrap stable libraries.
- **Fewer moving parts**: Don't add a service, module, queue or copy of data when an existing one can own it. Data that must change together belongs in one service and one transaction.
- **Simplify as you plan**: When planning, collect what could be simplified or removed. Step 20.7 in [PLAN.md](PLAN.md) lists the input for the codebase simplification review.
