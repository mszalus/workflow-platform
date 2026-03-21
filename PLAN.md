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

## Step 10: BPMN Import/Export — TODO

Add upload (import) and download (export) functionality to the Process Designer.

**Import (upload .bpmn file):**
- Add a file input / "Import BPMN" button to the Process Designer toolbar
- Use `FileReader` to read the uploaded `.bpmn` file as XML
- Call `modeler.importXML(xml)` to load it into the bpmn-js editor
- User can then edit and deploy as usual

**Export (download .bpmn file):**
- Add a "Download BPMN" button to the Process Designer toolbar
- Call `modeler.saveXML({ format: true })` to get the current diagram XML
- Create a `Blob` and trigger a browser download with `.bpmn` extension

**Files to modify:**
- `frontend/packages/bpmn-editor/src/BpmnEditor.tsx` — expose import/export methods or callbacks
- `frontend/apps/admin-portal/src/pages/ProcessDesigner.tsx` — add Import/Export buttons to toolbar

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

## Commit strategy

One commit after steps 1-4 (fixes + verified Docker stack), one commit for README, one for Helm fixes if any.

**Updated strategy (due to disk blocker):** Commit all current fixes together since Docker runtime verification is blocked. The fixes are all code-correct (verified via build/typecheck), just awaiting Docker runtime E2E verification.
