# Remaining Work — Execution Plan

## Active Tasks

Live cross-session task tracker. Items are removed once verified done (completed work lives in git log and the step records below). Kept in sync with the in-session task list.

- [ ] **Fix shell delegation bypass in `guard_git_subcommand.py`** — handle absolute shell path (`/bin/bash -c`) via `rsplit` on `segment[0]`; walk past flags to locate `-c` (`bash --login -c`). Add regression cases.
- [ ] **Add `+` refspec force-push deny rules to `.claude/settings.json`** — `Bash(git push origin +main*)`, `Bash(git push origin +HEAD:main*)`, `Bash(git push origin +refs/heads/main*)`.
- [ ] **Commit, push, and verify CI green for round-2 fixes**.

## Context

The workflow platform (Phase 1 + Phase 2) is built and verified with basic E2E testing (workflow-service + audit-service). This plan covers completing all remaining work: runtime testing of untested services, gateway routing, Docker full-stack build, README, and Helm deployment.

---

## Step 1: Fix Backend Dockerfiles — DONE

4 service Dockerfiles used `../../` relative COPY paths which break with docker-compose `context: ..` (project root).

**Files fixed:**
- `services/workflow-service/Dockerfile`
- `services/custom-fields-service/Dockerfile`
- `services/notification-service/Dockerfile`
- `services/audit-service/Dockerfile`
- `services/gateway/Dockerfile` (also fixed: was missing `buildSrc/` and `gradle.properties`)

**Additional fixes discovered during build:**
- All Dockerfiles must `COPY services/ services/` (not just target service) because `settings.gradle` includes all modules and Gradle requires all project directories to exist
- Gateway Dockerfile switched to alpine images for consistency
- Gateway Dockerfile: `groupadd`/`useradd` → `addgroup`/`adduser` (alpine)

**Verified:** All 7 Docker images (5 backend + 2 frontend) build successfully.

---

## Step 2: Runtime test custom-fields-service & notification-service — DONE (with fixes)

Start infra (PG on 5433, RabbitMQ, Keycloak) + all 5 backend services locally.

**Runtime bugs found and fixed:**
- `notification-service`: Duplicate `@FilterDef(name = "tenantFilter")` on both `Notification` and `NotificationPreference` entities. Fix: removed `@FilterDef` from `NotificationPreference`, kept only `@Filter`.
- `gateway`: `Failed to configure a DataSource` — gateway pulls in `spring-boot-starter-data-jpa` transitively via `wfp-security` but has no database. Fix: added `@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})`.
- `gateway`: `Predicate must not be null` — docker-compose env vars using indexed route overrides (`SPRING_CLOUD_GATEWAY_MVC_ROUTES_0_URI`) were creating partial route definitions without predicates. Fix: switched to named env vars (`WORKFLOW_SERVICE_URL`, etc.) with `${...}` defaults in `application.yml`.

**Additional runtime bugs found and fixed (2026-03-21):**
- `GlobalExceptionHandler`: `NoResourceFoundException` (trailing-slash URLs) returned 500 instead of 404. Fix: added explicit `@ExceptionHandler(NoResourceFoundException.class)` returning 404.
- `WorkflowEventListener` (notification-service): `handleTaskCompleted` crashed when `userId` was null (NOT NULL constraint on `notification.user_id`). Fix: added null guard + `completedBy` fallback.

**Keycloak realm-export.json fix (2026-03-21):**
- JWT tokens were missing `preferred_username`, `given_name`, `family_name`, `email`, and `realm_access.roles` claims
- Cause: realm-export.json only defined the `tenant` client scope; built-in `profile`/`email` scopes referenced in `defaultClientScopes` were not present because Keycloak 25 `start-dev --import-realm` doesn't auto-create built-in scopes for imported realms
- Fix: explicitly defined `profile`, `email`, and `roles` client scopes with protocol mappers in realm-export.json
- Impact: notification-service uses `preferred_username` to query/create notifications per user — was broken without this claim

**Verified via Docker runtime:**
- custom-fields-service: GET /api/schemas — 200 OK
- notification-service: GET /api/notifications — 200 OK (5 notifications created during E2E flow)
- notification-service: GET /api/notifications/unread-count — 200 OK (`{"count":5}`)
- notification-service: PUT /api/notifications/mark-all-read — 200 OK (count drops to 0)
- Note: trailing-slash paths (`/api/notifications/`) now return 404 (correct Spring Boot 3.x behavior) instead of 500

---

## Step 3: Gateway routing test — DONE

**Fixed:** Gateway used `StripPrefix=2` which stripped too many path segments.
- `/api/workflow/deployments` → `/deployments` (wrong, expects `/api/deployments`)

**New routing config:**
- `workflow-service`: `RewritePath=/api/workflow(?:/(?<segment>.*))?$, /api/${segment}`
- `custom-fields-service`: `RewritePath=/api/fields(?:/(?<segment>.*))?$, /api/${segment}`
- `notification-service`: pass-through (no filter — gateway path matches service path)
- `audit-service`: pass-through (no filter — gateway path matches service path)

**Also fixed:** Gateway env var approach changed from indexed (`SPRING_CLOUD_GATEWAY_MVC_ROUTES_N_URI`) to named (`WORKFLOW_SERVICE_URL`) to avoid partial route override issues.

**Runtime verified (2026-03-21):**
- Gateway → workflow-service (`/api/workflow/deployments`): 200 OK
- Gateway → custom-fields-service (`/api/fields/schemas?processDefinitionKey=test`): 200 OK
- Gateway → notification-service (`/api/notifications/unread-count`): 200 OK
- Gateway → audit-service (`/api/audit`): 200 OK

---

## Step 4: Docker full-stack build & E2E — DONE

1. ~~Fix Dockerfiles (Step 1)~~ DONE
2. ~~Build all 7 custom images~~ DONE
3. ~~All 10 containers running and healthy~~ DONE
4. ~~E2E flow through gateway~~ DONE

**E2E flow verified (2026-03-21):**
- Deploy BPMN (`POST /api/workflow/deployments`): 201 Created
- Start process (`POST /api/workflow/processes`): 201 Created (approvalProcess, businessKey=E2E-001)
- Complete "Submit Request" task: 204
- Claim + complete "Manager Approval" with `approved=true`: 204
- Process completed (no longer in active list)
- Audit trail captured all events: process.started, task.assigned, task.created, task.completed (9 total entries)

---

## Step 5: README documentation — DONE

Created `README.md` at project root with:
- Architecture diagram (text-based)
- Tech stack table
- Prerequisites
- Quick start (docker-compose)
- Local development setup (backend + frontend)
- API endpoints table (all gateway routes)
- Project structure tree
- Multi-tenancy explanation
- Testing and CI/CD sections
- Kubernetes/Helm deployment

---

## Step 9: Documentation — User Manual & Admin Manual — DONE

**User Manual** (`docs/user-manual.md`):
- Getting started, login flow (Keycloak OIDC), dashboard
- Task inbox: viewing, opening, completing tasks with custom fields and comments
- Starting a new process, tracking process instances
- Notifications: viewing, marking as read
- Multi-tenant isolation explanation, troubleshooting guide

**Admin Manual** (`docs/admin-manual.md`):
- Architecture diagram with gateway routing table
- Process Designer: BPMN editor with Flowable properties panel (user task, service task, async)
- Process definition management (deploy, delete)
- Custom field schemas: creating, deleting, field types
- Audit log: filtering by type/user, pagination, event types
- Keycloak administration: realm structure, user management, tenant isolation, JWT claims
- RabbitMQ monitoring: exchanges, queues, troubleshooting
- Docker deployment: container overview, startup order, environment variables, database schemas
- Kubernetes/Helm deployment: chart structure, install commands
- Comprehensive troubleshooting table and useful curl commands

**Note:** Screenshots not included (Playwright MCP browser launch conflicts with existing Chrome session). Keycloak login screenshot captured at `docs/screenshots/keycloak-login.png`.

---

## Step 10: BPMN Import/Export — DONE

Added upload (import), download (export), and edit-existing functionality to the Process Designer.

**Backend:**
- `DeploymentController.java` — added `GET /api/deployments/{processDefinitionId}/bpmn` endpoint
- `DeploymentService.java` — added `getProcessDefinitionBpmnXml()` method using Flowable `RepositoryService.getResourceAsStream()`

**Frontend:**
- `ProcessDesigner.tsx` — complete rewrite with Import (file upload via FileReader), Export (Blob download), and Edit (load existing BPMN from Flowable when URL has `:id` param). Auto-fills process name from filename on import.
- `ProcessList.tsx` — added Edit link for each process definition, linking to `/processes/designer/:id`

**Round-trip verification (all PASS):**
1. Deploy BPMN with 6 Flowable properties via curl → retrieve XML → all properties preserved
2. Re-deploy retrieved XML → retrieve again → all properties still intact
3. Browser: open existing process via Edit → Flowable properties panel shows all values correctly (assignee, candidateGroups, formKey, priority, async, class)
4. Browser: export XML from editor → all `flowable:*` attributes present in output
5. Browser: import exported file into fresh designer → all Flowable properties visible in panel

---

## Step 6: Helm deployment — BLOCKED (no helm binary)

Helm is not installed on this machine. Sub-chart lint passes in CI (GitHub Actions installs helm via `azure/setup-helm@v4`).

**TODO when helm is available:**
1. `helm dependency update helm/workflow-platform/`
2. `helm lint helm/workflow-platform/ -f helm/workflow-platform/values-local.yaml`
3. If minikube available: `helm install wfp helm/workflow-platform/ -f helm/workflow-platform/values-local.yaml`

---

## CI Pipeline Fixes — DONE

Two issues causing CI failures on every push to `main`:

1. **`gradlew` not executable** (exit code 126): File was committed with `100644` mode. Fix: `git update-index --chmod=+x gradlew` → now `100755`.
2. **Frontend typecheck fails in CI** (clean checkout has no `dist/`): Packages used `composite: true` with project references, requiring built `.d.ts` output that doesn't exist in CI. Fix: removed `composite`/`declaration`/`declarationMap` from shared-ui and bpmn-editor tsconfigs, removed `references` from admin-portal and user-portal tsconfigs. Vite resolves imports from source via npm workspace symlinks — no build artifacts needed.

**Verified:** `npm run typecheck --workspaces --if-present` passes with `dist/` directories deleted.

---

## Step 7: Playwright E2E Tests — DONE

Playwright test suite in `e2e/` directory. All 8 tests pass against live Docker stack.

**Files:**
- `e2e/playwright.config.ts` — config (baseURL: localhost:9080, 30s timeout)
- `e2e/playwright.test.ts` — 8 test cases
- `e2e/package.json` — standalone package with `@playwright/test`

**Test results (2026-03-21, all pass):**
1. Keycloak realm exists — OIDC discovery endpoint returns issuer
2. RabbitMQ management accessible — login + Overview page
3. Admin Portal loads and redirects to Keycloak — OIDC redirect with correct client_id
4. User Portal loads and redirects to Keycloak — OIDC redirect with correct client_id
5. Gateway health check — actuator/health returns UP
6. All backend services healthy — ports 8081-8084 all UP
7. Full workflow E2E through gateway — deploy → start → list tasks → complete → audit → notifications
8. Multi-tenant isolation — tenant-a and tenant-b see only their own processes

**Fixes applied to make tests pass:**
- RabbitMQ login: switched from `#username`/`#password` CSS selectors to role-based `getByRole('textbox')` (RabbitMQ management UI doesn't use ID attributes)
- Frontend portals: OIDC-protected apps redirect to Keycloak, so tests verify the redirect URL and Keycloak login page instead of checking for `#root`
- RabbitMQ Overview assertion: used `getByRole('heading')` to disambiguate from nav link

---

## Step 8: Flowable BPMN Editor Extensions — DONE

Added native Flowable support to bpmn-js editor. Produces `flowable:*` XML attributes directly.

**Approach:** Flowable moddle descriptor + custom properties panel provider.

**Files created/modified:**
- `frontend/packages/bpmn-editor/src/flowable.json` — Flowable moddle descriptor (~200 lines). Defines `flowable:` namespace and extensions:
  - `Assignable` (UserTask): assignee, candidateUsers, candidateGroups, dueDate, priority, formKey, category, skipExpression
  - `AsyncCapable` (Activity/Gateway/Event): async, asyncBefore, asyncAfter, exclusive
  - `ServiceTaskLike` (ServiceTask): class, delegateExpression, expression, resultVariable, type
  - `ScriptTaskLike`, `CallActivityLike`, `ProcessLike` extensions
  - `ExecutionListener`, `TaskListener`, `FormProperty` element types
- `frontend/packages/bpmn-editor/src/FlowablePropertiesProvider.ts` — registers Flowable property groups:
  - **Flowable** group on UserTask: assignee, candidate users/groups, form key, due date, priority
  - **Flowable** group on ServiceTask: java class, expression, delegate expression, result variable
  - **Asynchronous** group on all Activities/Gateways/Events: async, asyncBefore, asyncAfter, exclusive
- `frontend/packages/bpmn-editor/src/BpmnEditor.tsx` — updated to mount properties panel sidebar (320px), register Flowable moddle and provider modules
- `frontend/packages/bpmn-editor/src/types.d.ts` — type declarations for bpmn-js, properties-panel modules
- `frontend/apps/admin-portal/src/env.d.ts` — ambient module declarations for admin-portal TypeScript

**Verification:** `npm run typecheck --workspaces --if-present` passes. Runtime verification pending (needs frontend dev server or Docker rebuild).

---

## Frontend API Path Bug Fix — DONE (2026-03-21)

**Bug:** All frontend API calls used doubled `/api` prefix. The axios client has `baseURL: '/api'`, but every call also included `/api/` in the path (e.g., `apiClient.get('/api/workflow/deployments')` → request to `/api/api/workflow/deployments`).

**Additional issue:** Notification and audit paths were also doubled at the service level: `/api/notifications/notifications` and `/api/audit/audit`.

**Fix:** Removed `/api` prefix from all 22 API calls across 11 frontend files. Paths now use relative service paths (e.g., `/workflow/deployments`, `/notifications`, `/audit`).

**Files fixed (admin-portal):**
- `Dashboard.tsx`, `ProcessList.tsx`, `ProcessDesigner.tsx`, `CustomFieldEditor.tsx`, `AuditLog.tsx`

**Files fixed (user-portal):**
- `Dashboard.tsx`, `TaskInbox.tsx`, `TaskDetail.tsx`, `StartProcess.tsx`, `MyProcesses.tsx`, `Notifications.tsx`, `DynamicFieldForm.tsx`

**Verified:**
- TypeScript typecheck passes
- Docker frontend images rebuilt and restarted
- Nginx proxy paths verified via curl: admin-portal:5173/api/* and user-portal:5174/api/* → gateway → backend services (all 200 OK)
- All 8 Playwright E2E tests pass

---

## Step 11: Comprehensive E2E Testing & Bug Fixes — DONE (2026-03-21)

Extensive API and UI testing with Playwright MCP and curl. Found and fixed multiple critical bugs.

**Bugs found and fixed:**

1. **Flowable initiator resolution (500 on POST /api/processes):**
   - Root cause: ProcessService didn't call `identityService.setAuthenticatedUserId()` before starting process, and BPMN startEvent lacked `flowable:initiator="initiator"` attribute
   - Fix: Inject IdentityService with try/finally cleanup, add flowable:initiator to sample-approval.bpmn20.xml

2. **TaskDto missing processDefinitionKey:**
   - Frontend `Task` type expected it but backend didn't provide it
   - Fix: Added field to TaskDto, extract key from processDefinitionId (format: `key:version:uuid`) in TaskService and ProcessHistoryService

3. **FieldSchemaController 500 without processDefinitionKey:**
   - `@RequestParam` was required by default; admin Dashboard called without parameter
   - Fix: Made param optional, added `listAll()` to FieldSchemaService, added repository query

4. **Admin Dashboard hardcoded metrics:**
   - "Active Instances" and "Custom Field Schemas" showed "--"
   - Fix: Added real API calls to fetch data

5. **TaskInbox only showed assigned tasks:**
   - No way to see/claim candidate tasks
   - Fix: Added "Available to Claim" section with unassigned tasks and Claim button

6. **Error handling: Flowable exceptions returned 500:**
   - FlowableException (invalid BPMN), FlowableObjectNotFoundException, HttpMediaTypeNotSupportedException, MissingServletRequestParameterException all fell through to generic 500
   - Fix: Added FlowableExceptionHandler in workflow-service (400/404), added handlers in GlobalExceptionHandler for MissingParam, IllegalArgument, HttpMediaType

**Comprehensive E2E test suite:** `e2e/comprehensive.test.ts` — 71 tests across 13 categories covering health checks, auth, process lifecycle, approval flow, comments, custom fields, notifications, audit trail, multi-tenant isolation, negative tests, BPMN import/export, admin portal UI, and user portal UI.

**CI:** All 4 jobs pass (backend-build, frontend-build, docker-build, helm-lint).

---

## Verification

After each step, verify before moving to the next:
- Step 1: `docker compose -f docker/docker-compose.yml config --quiet` — PASSED
- Step 2: curl all custom-fields + notification endpoints with JWT — PASSED
- Step 3: curl through gateway for all 4 downstream services — PASSED
- Step 4: `docker compose up` + E2E flow — PASSED
- Step 5: README exists and is accurate — DONE
- Step 6: `helm lint` passes — BLOCKED (no helm)
- Step 7: All 8 Playwright E2E tests pass — VERIFIED
- Step 9: User + Admin manuals created — DONE
- CI fixes: typecheck passes without dist/ — VERIFIED
- Frontend API path fix: all proxy paths verified via curl — VERIFIED

## Step 12: Architecture Diagrams & Data Model — DONE (2026-04-05)

Created C4 model diagrams and data model documentation in `docs/architecture/` using Mermaid format (editable, version-controlled, GitHub-rendered).

**Files created:**
- `docs/architecture/README.md` — Index of all diagrams with viewing/editing instructions
- `docs/architecture/c4-context.md` — C4 Level 1: System Context (users, external systems, platform boundary)
- `docs/architecture/c4-container.md` — C4 Level 2: All containers (10 services, DB, MQ, gateway routing table, event flows)
- `docs/architecture/c4-component-workflow-service.md` — C4 Level 3: Workflow service internals (controllers, services, Flowable engine, event publisher)
- `docs/architecture/c4-component-notification-service.md` — C4 Level 3: Notification service (event listener, CRUD, event-to-notification mapping)
- `docs/architecture/c4-component-gateway.md` — C4 Level 3: Gateway (JWT validation, tenant propagation, routing)
- `docs/architecture/c4-deployment.md` — C4 Level 4: Docker Compose topology, GCP VM deployment, Kubernetes/Helm with resource allocation
- `docs/architecture/data-model.md` — ER diagram: 9 JPA entities + key Flowable tables, enumerations, cross-schema references, multi-tenancy pattern

Each diagram includes a "Notes for Editors" section explaining how to extend it for common changes (add service, add entity, add route, etc.).

---

## Step 13: SDLC Improvements — DONE (2026-04-06)

All 9 phases of the SDLC improvements plan implemented and committed.

**Phases 1–8 (previously committed):**
- Phase 1: ESLint 9 (flat config) + Prettier 3 — frontend linting and formatting
- Phase 2: Checkstyle 10 — Java style enforcement across all modules
- Phase 3: Husky + lint-staged — pre-commit hooks
- Phase 4: Commitlint — conventional commit message enforcement
- Phase 5a: OWASP dependency-check + npm audit — security scanning
- Phase 5b: Trivy — container image CVE scanning in CI
- Phase 6: Docker hardening — all containers run as non-root (appuser / nginx-unprivileged)
- Phase 7: JaCoCo — test coverage reporting (XML + HTML)
- Phase 8: CSRF comment in SecurityConfig, branch protection documented

**Phase 9 (committed 2026-04-06, 5 commits):**
- Removed all Checkstyle suppressions one service at a time
- Fixed every violation: star imports → explicit imports, LeftCurly, NeedBraces, unused imports, long lines
- Deleted `config/checkstyle/suppressions.xml` and removed SuppressionFilter from `checkstyle.xml`
- All 30 `checkstyleMain` tasks pass with zero violations

---

## Step 14: Observability & BDD Acceptance Tests — DONE (2026-04-09)

**Observability (commits 45349b8, 02052a6):**

Structured JSON logging, distributed tracing via OpenTelemetry, and Prometheus metrics added to all five services. Ready for GCP Cloud Logging, Cloud Trace, and Google Managed Prometheus without code changes — only env-var overrides.

- `logstash-logback-encoder` + `logback-spring.xml` on all services: JSON to stdout (`!local` profile), colored console for `local` profile
- `GcpLoggingJsonProvider` (wfp-common): maps WARN→WARNING for GCP severity; adds `logging.googleapis.com/trace` + span fields when `GOOGLE_CLOUD_PROJECT` is set
- `TenantInterceptor` writes `tenantId` and `userId` to MDC on every request
- `micrometer-registry-prometheus`: `/actuator/prometheus` on all services
- `micrometer-tracing-bridge-otel` + `opentelemetry-exporter-otlp`: traces sent to `OTEL_EXPORTER_OTLP_ENDPOINT` (default `http://localhost:4318`)
- Local stack added to docker-compose: OTEL Collector (4317/4318) → Tempo → Grafana (3000); Prometheus (9090) → Grafana; both datasources auto-provisioned
- Helm deployment templates: `prometheus.io/scrape` annotations on all pods; `OTEL_EXPORTER_OTLP_ENDPOINT` and `MANAGEMENT_TRACING_SAMPLING_PROBABILITY=0.1` in per-service values
- `values-gcp.yaml`: documents Cloud Trace (OTLP collector swap), GMP (annotations already present), Cloud Logging (set `GOOGLE_CLOUD_PROJECT`)

**BDD Acceptance Tests (commits 1f4f822, 30de72d, 6cc3f4e, 0f1cc94):**

20 Cucumber scenarios across 4 phases, all passing:
- Phase A: process management + task lifecycle (8 scenarios)
- Phase B: multi-tenancy isolation, audit trail, API security (8 scenarios)
- Phase C: custom fields, notifications via RabbitMQ (4 scenarios)
- Phase D: `acceptance-tests` CI job (builds images, starts stack, runs BDD, uploads report)

---

## Step 15: Local Observability Verification

**Goal:** Verify that traces appear in Tempo, metrics appear in Prometheus, and structured logs contain the right fields — all running locally via docker-compose.

### Prerequisites

- Full stack running: `docker compose -f docker/docker-compose.yml up -d`
- Wait for all services to be healthy (gateway health: `curl http://localhost:9080/actuator/health`)
- New containers added: `wfp-otel-collector`, `wfp-tempo`, `wfp-prometheus`, `wfp-grafana`
- Note: backend service images must be rebuilt after the observability commit to pick up new JARs:
  ```bash
  docker compose -f docker/docker-compose.yml build \
    gateway workflow-service custom-fields-service notification-service audit-service
  docker compose -f docker/docker-compose.yml up -d
  ```

### 15.1 Verify Prometheus scraping

1. Open `http://localhost:9090/targets` — all 5 services + `otel-collector` must show **State: UP**
2. If any show DOWN, check `docker logs wfp-prometheus` and verify the service container is running
3. Spot-check a metric in the Prometheus query UI:
   ```promql
   http_server_requests_seconds_count{application="workflow-service"}
   ```
4. Generate traffic first if needed:
   ```bash
   TOKEN=$(curl -s -X POST http://localhost:8180/realms/workflow-platform/protocol/openid-connect/token \
     -d "grant_type=password&client_id=wfp-admin-portal&username=admin-a&password=password" \
     | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")
   curl -s -H "Authorization: Bearer $TOKEN" http://localhost:9080/api/workflow/deployments | python3 -m json.tool
   ```
5. Query JVM metrics: `jvm_memory_used_bytes{application="workflow-service"}`

### 15.2 Verify distributed tracing in Grafana + Tempo

1. Open `http://localhost:3000` (anonymous access, no login required)
2. Go to **Explore** → select **Tempo** datasource
3. Set **Query type: Search**, click **Run query** — traces from all services should appear
4. Click any trace to see the span waterfall: gateway → workflow-service (or whichever service handled the request)
5. Verify span attributes include `tenantId` (set via MDC) and `http.route`
6. In the **Grafana Explore** panel, switch to **Prometheus** datasource and verify `up` metric shows all targets

### 15.3 Verify structured logs

1. Inspect a backend service container's stdout:
   ```bash
   docker logs wfp-workflow --tail 20
   ```
   Each line should be a single JSON object with fields: `time`, `severity`, `message`, `logger`, `thread`, `service`, `traceId`, `spanId`, `tenantId`, `userId`

2. Verify `severity` uses GCP values (INFO, WARNING, ERROR — not WARN):
   ```bash
   docker logs wfp-workflow 2>&1 | python3 -c "
   import sys, json
   for line in sys.stdin:
       try:
           obj = json.loads(line)
           print(obj.get('severity'), '|', obj.get('tenantId'), '|', obj.get('message','')[:60])
       except: pass
   " | head -20
   ```

3. Trigger a WARN-level log by making an unauthenticated request and verify `severity: WARNING` appears (not WARN):
   ```bash
   curl -s http://localhost:9080/api/workflow/deployments  # no token → 401
   docker logs wfp-gateway --tail 5
   ```

4. Verify `tenantId` appears on authenticated requests:
   ```bash
   curl -s -H "Authorization: Bearer $TOKEN" http://localhost:9080/api/workflow/deployments > /dev/null
   docker logs wfp-workflow --tail 5 | python3 -c "import sys,json; [print(json.loads(l).get('tenantId','(none)')) for l in sys.stdin if l.strip()]"
   ```

### 15.4 Verify trace–log correlation (manual)

1. Make an API call and note the `traceId` from the response log:
   ```bash
   curl -s -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"processDefinitionKey":"test"}' \
     http://localhost:9080/api/workflow/processes
   docker logs wfp-workflow --tail 3 | python3 -c "import sys,json; [print(json.loads(l).get('traceId')) for l in sys.stdin if l.strip()]"
   ```
2. Take the `traceId`, open Grafana Explore → Tempo → **TraceQL** → `{ .traceId = "<id>" }` — the full trace should appear

### 15.5 Run BDD acceptance tests to confirm nothing regressed

```bash
JAVA_HOME='C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.4\jbr' \
  ./gradlew :tests:bdd-acceptance:test --no-daemon
```
All 20 scenarios must pass.

**Verification checklist:**
- [ ] Prometheus shows all 6 scrape targets as UP
- [ ] Grafana Tempo shows traces with multi-span waterfalls
- [ ] Container logs are JSON with `severity`, `traceId`, `tenantId`, `userId`
- [ ] `severity` uses GCP values (WARNING not WARN)
- [ ] All 20 BDD scenarios pass

---

## Step 16: GCP Infrastructure — Terraform + Helm Preparation

**Goal:** Create all GCP infrastructure as code so the platform can be deployed to GCP with a single `terraform apply` + `helm install`. No deployment happens in this step — only code is written and reviewed.

### 16.1 GCP APIs to enable (one-time, per project)

```bash
gcloud services enable \
  container.googleapis.com \
  sqladmin.googleapis.com \
  secretmanager.googleapis.com \
  artifactregistry.googleapis.com \
  cloudtrace.googleapis.com \
  monitoring.googleapis.com \
  logging.googleapis.com \
  dns.googleapis.com \
  certificatemanager.googleapis.com
```

### 16.2 Terraform structure to create

```
terraform/
├── modules/
│   ├── gke/           # GKE cluster + node pools
│   ├── cloudsql/      # PostgreSQL 16 instance + databases + users
│   ├── artifact-registry/   # Docker image repository
│   ├── secrets/       # Secret Manager entries (DB password, RabbitMQ creds, Keycloak admin)
│   ├── iam/           # Service accounts + Workload Identity bindings
│   └── networking/    # VPC, subnets, Cloud NAT, firewall rules
├── environments/
│   ├── dev/           # dev tfvars + state backend config
│   └── prod/          # prod tfvars + state backend config
├── main.tf
├── variables.tf
└── outputs.tf         # Outputs: cluster name, SQL connection name, registry URL
```

**Key Terraform resources:**

| Resource | Type | Notes |
|---|---|---|
| GKE cluster | `google_container_cluster` | Autopilot for dev; Standard n2-standard-4 for prod |
| Cloud SQL | `google_sql_database_instance` | PostgreSQL 16, private IP via VPC peering |
| Artifact Registry | `google_artifact_registry_repository` | Docker format, region-specific |
| Secret Manager | `google_secret_manager_secret` | DB password, RabbitMQ password, Keycloak admin, JWT secret |
| Workload Identity | `google_service_account` + `google_iam_binding` | Pods write to Cloud Trace + Cloud Logging without key files |
| VPC | `google_compute_network` | Private cluster, no public node IPs |
| Cloud NAT | `google_compute_router_nat` | Outbound internet for pods (pull images, reach Keycloak) |
| Managed cert | `google_compute_managed_ssl_certificate` | TLS for the gateway ingress |

### 16.3 Helm changes required before GCP deployment

**a) External Secrets Operator (ESO)**

Install ESO to bridge Secret Manager → K8s Secrets. Add `ExternalSecret` CRDs for:
- `wfp-db-credentials` (DB username/password)
- `wfp-rabbitmq-credentials`
- `wfp-keycloak-admin`

Services reference these as `envFrom.secretRef` instead of plaintext env vars.

**b) cert-manager + Ingress**

Install `cert-manager` (via Helm) with `ClusterIssuer` pointing to Let's Encrypt (or Google CA). Update gateway Helm chart to add:
```yaml
ingress:
  enabled: true
  className: gce           # GKE Ingress controller
  annotations:
    kubernetes.io/ingress.global-static-ip-name: wfp-gateway-ip
    networking.gke.io/managed-certificates: wfp-tls-cert
  hosts:
    - host: api.yourdomain.com
      paths: [{path: /, pathType: Prefix}]
```

**c) OTEL Collector for Cloud Trace**

Add a `wfp-otel-collector` Deployment (or DaemonSet) to the Helm umbrella chart with:
```yaml
config:
  exporters:
    googlecloud:
      project: ${GOOGLE_CLOUD_PROJECT}
  service:
    pipelines:
      traces:
        exporters: [googlecloud]
```
All backend service pods set `OTEL_EXPORTER_OTLP_ENDPOINT: http://wfp-otel-collector:4318`.

**d) Keycloak production mode**

The current docker-compose uses `start-dev`. For GCP, Keycloak must use `start --optimized` with a pre-built image. Add a `keycloak` sub-chart to the Helm umbrella (or use Bitnami Keycloak chart) with:
- `KC_DB`: Cloud SQL PostgreSQL via Cloud SQL Auth Proxy sidecar
- `KC_HOSTNAME`: the public Keycloak hostname (e.g., `auth.yourdomain.com`)
- Realm import via init container or import job

**e) RabbitMQ with persistence**

Replace `tmpfs` with persistent volumes. Use Bitnami RabbitMQ Helm chart with quorum queues enabled. Or consider GCP-managed alternatives (Google Cloud Pub/Sub with a bridge if scale demands it — this is a larger architectural change).

**f) Values files**

Update `helm/workflow-platform/values-gcp.yaml` with:
- `GOOGLE_CLOUD_PROJECT` env var on all services (enables Cloud Logging trace linking)
- `OTEL_EXPORTER_OTLP_ENDPOINT: http://wfp-otel-collector:4318`
- Image tags pointing to Artifact Registry (`europe-west1-docker.pkg.dev/<project>/wfp/<service>:<tag>`)
- `replicaCount: 2` minimum on all stateless services
- Resource requests/limits tuned to actual load test results

### 16.4 CI/CD pipeline additions

1. **Image build + push job**: On merge to `main`, build Docker images and push to Artifact Registry with commit SHA as tag
2. **Helm diff job**: On PR, run `helm diff upgrade` against the dev cluster (read-only) to show what would change
3. **Deploy to dev job**: On merge to `main`, `helm upgrade --install wfp ... --set image.tag=$SHA`
4. **Deploy to prod job**: Manual trigger or tag-based, with approval gate

**Verification checklist for this step:**
- [ ] `terraform validate` passes on all modules
- [ ] `terraform plan` against an empty GCP project produces the expected resource list
- [ ] `helm lint helm/workflow-platform/ -f helm/workflow-platform/values-gcp.yaml` passes
- [ ] ESO ExternalSecrets manifests validated with `kubectl apply --dry-run=server`
- [ ] `helm template` output reviewed for any hardcoded localhost references

---

## Step 17: GCP Deployment and Acceptance Testing

**Goal:** Deploy the full platform to GCP, run the BDD acceptance tests against it, and verify observability (Cloud Logging, Cloud Trace, Cloud Monitoring).

### 17.1 One-time infrastructure bootstrap

```bash
# Authenticate
gcloud auth application-default login
gcloud config set project YOUR_PROJECT_ID

# Create GCS bucket for state (one-time)
gsutil mb -l europe-west1 gs://YOUR_PROJECT_ID-tfstate
gsutil versioning set on gs://YOUR_PROJECT_ID-tfstate

# Copy and fill in dev tfvars
cp terraform/environments/dev/terraform.tfvars.example terraform/terraform.tfvars
# Edit terraform/terraform.tfvars with real values

# Provision infrastructure (~10 min for full GKE + SQL)
cd terraform
terraform init -backend-config=environments/dev/backend.tf
terraform apply

# Configure kubectl
gcloud container clusters get-credentials wfp-dev \
  --region europe-west1 --project YOUR_PROJECT_ID

# Verify nodes are ready
kubectl get nodes
```

### 17.2 Build and push images to Artifact Registry

```bash
# Authenticate Docker to Artifact Registry
gcloud auth configure-docker europe-west1-docker.pkg.dev

# Build and push (or use CI — see Step 16.4)
REGISTRY=europe-west1-docker.pkg.dev/YOUR_PROJECT/wfp
TAG=$(git rev-parse --short HEAD)

for svc in gateway workflow-service custom-fields-service notification-service audit-service; do
  docker build -f services/$svc/Dockerfile -t $REGISTRY/$svc:$TAG .
  docker push $REGISTRY/$svc:$TAG
done
```

### 17.3 Install prerequisites

```bash
# cert-manager
helm install cert-manager jetstack/cert-manager \
  --namespace cert-manager --create-namespace \
  --set crds.enabled=true

# External Secrets Operator
helm install external-secrets external-secrets/external-secrets \
  --namespace external-secrets --create-namespace

# Create ClusterSecretStore pointing at Secret Manager
kubectl apply -f helm/cluster-secret-store.yaml
```

### 17.4 Deploy the platform

```bash
# Update Helm dependencies (Bitnami PostgreSQL, RabbitMQ, Keycloak sub-charts)
helm dependency update helm/workflow-platform/

# Deploy (first time)
helm install wfp helm/workflow-platform/ \
  -f helm/workflow-platform/values-gcp.yaml \
  --set-string "workflow-service.image.tag=$TAG" \
  --set-string "custom-fields-service.image.tag=$TAG" \
  --set-string "notification-service.image.tag=$TAG" \
  --set-string "audit-service.image.tag=$TAG" \
  --set-string "gateway.image.tag=$TAG" \
  --namespace wfp --create-namespace \
  --timeout 10m --wait

# Verify all pods are Running
kubectl get pods -n wfp
```

### 17.5 Run BDD acceptance tests against GCP

The existing BDD test suite runs against configurable endpoints. Point it at GCP:

```bash
JAVA_HOME='C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.4\jbr' \
  ./gradlew :tests:bdd-acceptance:test --no-daemon \
  -DGATEWAY_URL=https://api.yourdomain.com \
  -DKEYCLOAK_URL=https://auth.yourdomain.com
```

This requires updating `ApiClient.java` to read `GATEWAY_URL` and `KEYCLOAK_URL` from system properties (currently hardcoded to `http://localhost:9080`). That's a 2-line change.

Expected result: all 20 scenarios pass against GCP.

### 17.6 Verify observability on GCP

**Cloud Logging:**
1. GCP Console → Logging → Log Explorer
2. Filter: `resource.type="k8s_container" resource.labels.namespace_name="wfp"`
3. Verify JSON structure: `severity`, `message`, `service`, `traceId`, `tenantId`, `userId` fields
4. Click the Cloud Trace link on any log entry with a trace ID → opens the full trace

**Cloud Trace:**
1. GCP Console → Cloud Trace → Trace List
2. Filter by service name — you should see traces from gateway, workflow-service, etc.
3. Click any trace → span waterfall with latency breakdown per service
4. Verify tenant context propagates correctly across spans

**Cloud Monitoring (Google Managed Prometheus):**
1. GCP Console → Monitoring → Metrics Explorer
2. Select metric `prometheus.googleapis.com/http_server_requests_seconds_count/counter`
3. Filter by `application` label — one line per service
4. Create dashboards for RED metrics (Rate, Errors, Duration) per service

### 17.7 Teardown (after testing)

```bash
# Remove application
helm uninstall wfp --namespace wfp

# Destroy infrastructure (saves ~$5-8/day on dev)
cd terraform/environments/dev
terraform destroy -var-file=dev.tfvars
```

---

## GCP Cost Estimate

All prices approximate, us-central1 / europe-west1 regions, on-demand pricing (2026).

### Dev / Staging environment (minimal, one-shot testing)

| Resource | Spec | Monthly cost |
|---|---|---|
| GKE cluster management fee | 1 cluster (waived for first cluster per billing account) | $0–$73 |
| GKE nodes | 2 × e2-standard-2 (2 vCPU, 8 GB), Spot/preemptible | ~$25 |
| Cloud SQL | PostgreSQL 16, `db-f1-micro` (1 vCPU, 614 MB), no HA | ~$7 |
| Artifact Registry | ~5 GB image storage | ~$0.50 |
| Cloud Load Balancer | 1 L7 HTTP(S) LB | ~$18 |
| Cloud DNS | 1 managed zone | ~$0.50 |
| Cloud Logging | First 50 GB/month free | ~$0 |
| Cloud Trace | First 2.5M spans/month free | ~$0 |
| Cloud Monitoring | Free for GKE metrics | ~$0 |
| Network egress | ~10 GB/month | ~$1 |
| **Total (with free GKE mgmt)** | | **~$52/month** |
| **Total (without free tier)** | | **~$125/month** |

> **Note:** GCP gives new accounts $300 free credit (~3–6 months of dev environment).

### Production environment (minimal, 2 replicas, HA)

| Resource | Spec | Monthly cost |
|---|---|---|
| GKE cluster management | 1 cluster | ~$73 |
| GKE nodes | 3 × n2-standard-4 (4 vCPU, 16 GB), regular | ~$360 |
| Cloud SQL | PostgreSQL 16, `db-n1-standard-2` (2 vCPU, 7.5 GB), HA | ~$185 |
| Artifact Registry | ~20 GB | ~$0.40 |
| Cloud Load Balancer | 1 L7 HTTPS LB | ~$18 |
| Cloud Armor (WAF) | Basic tier | ~$5 |
| Cloud DNS | 1 zone | ~$0.50 |
| Cloud Logging | ~100 GB/month at scale | ~$25 |
| Cloud Trace | ~50M spans at moderate traffic | ~$25 |
| Cloud Monitoring | Custom metrics beyond free tier | ~$10 |
| Network egress | ~50 GB/month | ~$5 |
| **Total** | | **~$707/month** |

### Cost reduction levers

| Action | Saving |
|---|---|
| Use GKE Autopilot instead of Standard (for lower traffic) | 30–40% on node cost |
| Spot/preemptible nodes for non-prod workloads | 60–80% on node cost |
| Committed Use Discounts (1-year) | 37% on compute |
| Cloud SQL shared-core (`f1-micro`) in non-prod | Saves ~$175/month vs `n1-standard-2` |
| Scale down non-prod overnight (node pool min=0) | Saves ~60% on node cost |
| Use Cloud Run instead of GKE for stateless services | Pay only for requests, near-zero idle cost |

---

---

## Step 18: Release and Rollback Strategy

**Goal:** Define a safe, repeatable release process using Kubernetes native features (rolling updates, Helm revisions, replica-weighted canary) — no external tooling required.

### 18.1 Release strategy: rolling update (default)

All Helm charts are configured with `RollingUpdate` strategy (K8s default). A new release proceeds as:

```bash
# 1. Build and push new image to Artifact Registry
TAG=$(git rev-parse --short HEAD)
REGISTRY=europe-west1-docker.pkg.dev/YOUR_PROJECT/wfp-prod

docker build -f services/workflow-service/Dockerfile -t $REGISTRY/workflow-service:$TAG .
docker push $REGISTRY/workflow-service:$TAG

# 2. Upgrade via Helm (atomically upgrades, keeps old revision)
helm upgrade wfp helm/workflow-platform/ \
  -f helm/workflow-platform/values-gcp.yaml \
  --set-string "workflow-service.image.tag=$TAG" \
  --namespace wfp \
  --timeout 5m \
  --wait \
  --atomic   # rolls back automatically if pods fail to become ready
```

The `--atomic` flag means Helm will roll back to the previous revision if any pod fails readiness within `--timeout`. This prevents a bad release from staying stuck in half-upgraded state.

**Rolling update parameters** (already in chart `values.yaml`):
```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxSurge: 1        # one extra pod created before old one terminates
    maxUnavailable: 0  # no downtime — always at least N healthy pods
```

To add these to every chart's `deployment.yaml`, insert under `spec.strategy`:
```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxSurge: 1
    maxUnavailable: 0
```

### 18.2 Canary release: replica weighting

For higher-risk changes, use replica weighting to send a fraction of traffic to the new version before full rollout. This works with any K8s-native service (no service mesh required).

**Method:** Two Deployments, one Service, shared pod label selector.

```bash
# Step 1: Deploy canary (10% of replicas = 1 of 10 total)
kubectl apply -f - <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: workflow-service-canary
  namespace: wfp
spec:
  replicas: 1
  selector:
    matchLabels:
      app: wfp-workflow-service
      track: canary
  template:
    metadata:
      labels:
        app: wfp-workflow-service
        track: canary
    spec:
      containers:
        - name: workflow-service
          image: europe-west1-docker.pkg.dev/YOUR_PROJECT/wfp-prod/workflow-service:$NEW_TAG
          # ... same ports, env, probes as stable
EOF

# Step 2: Verify canary is healthy
kubectl rollout status deployment/workflow-service-canary -n wfp
kubectl logs -l track=canary -n wfp --tail=50

# Step 3: Check error rate in Cloud Monitoring
# Filter: resource.labels.pod_name =~ ".*-canary-.*"

# Step 4a: Promote — upgrade stable deployment to new tag
helm upgrade wfp helm/workflow-platform/ \
  -f helm/workflow-platform/values-gcp.yaml \
  --set-string "workflow-service.image.tag=$NEW_TAG" \
  --namespace wfp --wait --atomic

# Then delete the canary
kubectl delete deployment workflow-service-canary -n wfp

# Step 4b: Abort — delete canary if issues found
kubectl delete deployment workflow-service-canary -n wfp
# Stable deployment was never touched — no rollback needed
```

**Traffic split math:** The K8s Service selects all pods with `app: wfp-workflow-service` regardless of `track` label. With 9 stable replicas + 1 canary = 10% canary traffic. Adjust canary `replicas` for finer control.

### 18.3 Rollback procedure

#### Option A: Helm rollback (recommended for Helm-managed releases)

```bash
# List all revisions
helm history wfp --namespace wfp

# Rollback to previous revision (most common case)
helm rollback wfp --namespace wfp --wait

# Rollback to a specific revision
helm rollback wfp 3 --namespace wfp --wait
```

Helm rollback re-applies the previous `values.yaml` + templates, including image tags. GKE triggers a new rolling update back to the previous pod spec. No data migrations are reversed — database is NOT rolled back.

#### Option B: kubectl rollout (for hotfixes without Helm)

```bash
# See revision history for a deployment
kubectl rollout history deployment/wfp-workflow-service -n wfp

# Undo last rollout
kubectl rollout undo deployment/wfp-workflow-service -n wfp

# Undo to a specific revision
kubectl rollout undo deployment/wfp-workflow-service --to-revision=2 -n wfp

# Monitor rollback
kubectl rollout status deployment/wfp-workflow-service -n wfp
```

**Important:** `kubectl rollout undo` is bypassed by the next `helm upgrade` — use only for emergency hotfixes, then reconcile with Helm.

### 18.4 Database migration handling

Flyway runs at application startup. Since K8s rolling updates overlap old and new pods, migrations must be:

1. **Backwards compatible** — new schema changes must not break old pods still running
   - Add columns as nullable or with defaults (never drop columns in the same release)
   - Two-phase deploy: add column (release N) → backfill (release N) → make NOT NULL (release N+1)

2. **Idempotent** — Flyway `repair-on-migrate: true` (already configured) handles checksum mismatches

3. **Never rename or drop in a single release** — always expand-then-contract over two releases

**Migration rollback:** Flyway does not support automatic migration rollback. If a bad migration is applied:
- Fix forward: write a new migration that reverses the change
- Emergency: restore from Cloud SQL point-in-time backup (PITR) — enabled for HA/prod environments

### 18.5 Automated rollback triggers

Configure readiness probes tightly (already in charts). Add pod disruption budgets to prevent too many pods going down simultaneously:

```yaml
# Apply to each backend service namespace
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: wfp-workflow-service-pdb
  namespace: wfp
spec:
  minAvailable: 1
  selector:
    matchLabels:
      app: wfp-workflow-service
```

Combined with `--atomic` on `helm upgrade`, this ensures:
- Deployment fails fast if new pods don't pass readiness within timeout
- Helm automatically reverts to the last good revision
- Minimum service availability is maintained throughout

### 18.6 Release checklist

Before each production release:

- [ ] All BDD acceptance tests pass in dev/staging (`./gradlew :tests:bdd-acceptance:test`)
- [ ] New Flyway migrations are backwards compatible (tested against prod-snapshot DB)
- [ ] Helm dry-run shows expected changes: `helm upgrade --dry-run wfp ...`
- [ ] Canary deployed and healthy for ≥15 minutes before promotion
- [ ] Rollback procedure reviewed — know which `helm history` revision to target
- [ ] On-call engineer available for 30 minutes post-promotion
- [ ] Cloud Monitoring error rate alert threshold reviewed

---

## Commit strategy

One commit after steps 1-4 (fixes + verified Docker stack), one commit for README, one for Helm fixes if any.

**Updated strategy (due to disk blocker):** Commit all current fixes together since Docker runtime verification is blocked. The fixes are all code-correct (verified via build/typecheck), just awaiting Docker runtime E2E verification.
