---
title: Step 02 — Runtime test custom-fields-service and notification-service
tags:
  - project
  - plan
  - status/done
type: plan
source: PLAN.md
status: done
---
[[Project MOC]] › **Step 02 — Runtime test custom-fields-service and notification-service**

Start infra (PG on 5433, [[Event System|RabbitMQ]], [[Security and JWT|Keycloak]]) + all 5 backend services locally.

**Runtime bugs found and fixed:**
- `notification-service`: Duplicate `@FilterDef(name = "tenantFilter")` on both `Notification` and `NotificationPreference` entities. Fix: removed `@FilterDef` from `NotificationPreference`, kept only `@Filter`.
- `gateway`: `Failed to configure a DataSource` — gateway pulls in `spring-boot-starter-data-jpa` transitively via `wfp-security` but has no database. Fix: added `@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})`.
- `gateway`: `Predicate must not be null` — docker-compose env vars using indexed route overrides (`SPRING_CLOUD_GATEWAY_MVC_ROUTES_0_URI`) were creating partial route definitions without predicates. Fix: switched to named env vars (`WORKFLOW_SERVICE_URL`, etc.) with `${...}` defaults in `application.yml`.

**Additional runtime bugs found and fixed (2026-03-21):**
- `GlobalExceptionHandler`: `NoResourceFoundException` (trailing-slash URLs) returned 500 instead of 404. Fix: added explicit `@ExceptionHandler(NoResourceFoundException.class)` returning 404.
- `WorkflowEventListener` ([[Notification Service|notification-service]]): `handleTaskCompleted` crashed when `userId` was null (NOT NULL constraint on `notification.user_id`). Fix: added null guard + `completedBy` fallback.

**Keycloak realm-export.json fix (2026-03-21):**
- JWT tokens were missing `preferred_username`, `given_name`, `family_name`, `email`, and `realm_access.roles` claims
- Cause: realm-export.json only defined the `tenant` client scope; built-in `profile`/`email` scopes referenced in `defaultClientScopes` were not present because Keycloak 25 `start-dev --import-realm` doesn't auto-create built-in scopes for imported realms
- Fix: explicitly defined `profile`, `email`, and `roles` client scopes with protocol mappers in realm-export.json
- Impact: notification-service uses `preferred_username` to query/create notifications per user — was broken without this claim

**Verified via Docker runtime:**
- [[Custom Fields Service|custom-fields-service]]: GET /api/schemas — 200 OK
- notification-service: GET /api/notifications — 200 OK (5 notifications created during E2E flow)
- notification-service: GET /api/notifications/unread-count — 200 OK (`{"count":5}`)
- notification-service: PUT /api/notifications/mark-all-read — 200 OK (count drops to 0)
- Note: trailing-slash paths (`/api/notifications/`) now return 404 (correct Spring Boot 3.x behavior) instead of 500

---


---

**Plan** — ← [[Step 01 — Fix Backend Dockerfiles]] · [[Step 03 — Gateway routing test]] →

> [!abstract]- All notes in this set
> [[Active Tasks]]
> [[Project Context]]
> [[Step 01 — Fix Backend Dockerfiles]]
> [[Step 03 — Gateway routing test]]
> [[Step 04 — Docker full-stack build and E2E]]
> [[Step 05 — README documentation]]
> [[Step 09 — Documentation User Manual and Admin Manual]]
> [[Step 10 — BPMN Import Export]]
> [[Step 06 — Helm deployment]]
> [[CI Pipeline Fixes]]
> [[Step 07 — Playwright E2E Tests]]
> [[Step 08 — Flowable BPMN Editor Extensions]]
> [[Frontend API Path Bug Fix]]
> [[Step 11 — Comprehensive E2E Testing and Bug Fixes]]
> [[Verification Log]]
> [[Step 12 — Architecture Diagrams and Data Model]]
> [[Step 13 — SDLC Improvements]]
> [[Step 14 — Observability and BDD Acceptance Tests]]
> [[Step 15 — Local Observability Verification]]
> [[Step 16 — GCP Infrastructure Terraform Helm Preparation]]
> [[Step 17 — GCP Deployment and Acceptance Testing]]
> [[GCP Cost Estimate]]
> [[Step 18 — Release and Rollback Strategy]]
> [[Step 19 — GCP Observability Readiness no deployment]]
> [[Step 20 — Work Item Tracker on BPMN]]
> [[Commit Strategy]]
