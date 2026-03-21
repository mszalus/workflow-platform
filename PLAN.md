# Remaining Work — Execution Plan

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
- All Dockerfiles must `COPY services/ services/` (not just target service) because `settings.gradle.kts` includes all modules and Gradle requires all project directories to exist
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

## Step 7: Browser-based E2E testing (Playwright MCP) — DONE (curl-based)

Playwright MCP not available in CLI session. Verified all endpoints via curl instead.

**Results (2026-03-21):**
1. **Keycloak** — `http://localhost:8180/realms/workflow-platform/.well-known/openid-configuration`: 200 OK
2. **RabbitMQ management** — `http://localhost:15672/api/overview`: 200 OK. Verified: `wfp.events` exchange (topic), `wfp.audit` queue (`#`), `wfp.notification` queue (`task.*`, `process.completed`)
3. **Admin Portal** — `http://localhost:5173/`: 200 OK
4. **User Portal** — `http://localhost:5174/`: 200 OK
5. **Gateway health** — `http://localhost:9080/actuator/health`: `{"status":"UP"}`
6. **Service health checks** — all 4 services (8081-8084): `{"status":"UP"}`

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

## Verification

After each step, verify before moving to the next:
- Step 1: `docker compose -f docker/docker-compose.yml config --quiet` — PASSED
- Step 2: curl all custom-fields + notification endpoints with JWT — PASSED
- Step 3: curl through gateway for all 4 downstream services — PASSED
- Step 4: `docker compose up` + E2E flow — PASSED
- Step 5: README exists and is accurate — DONE
- Step 6: `helm lint` passes — BLOCKED (no helm)
- CI fixes: typecheck passes without dist/ — VERIFIED

## Commit strategy

One commit after steps 1-4 (fixes + verified Docker stack), one commit for README, one for Helm fixes if any.

**Updated strategy (due to disk blocker):** Commit all current fixes together since Docker runtime verification is blocked. The fixes are all code-correct (verified via build/typecheck), just awaiting Docker runtime E2E verification.
