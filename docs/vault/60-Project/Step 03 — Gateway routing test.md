---
title: Step 03 — Gateway routing test
tags:
  - project
  - plan
  - status/done
type: plan
source: PLAN.md
status: done
---
[[Project MOC]] › **Step 03 — Gateway routing test**

**Fixed:** Gateway used `StripPrefix=2` which stripped too many path segments.
- `/api/workflow/deployments` → `/deployments` (wrong, expects `/api/deployments`)

**New routing config:**
- `workflow-service`: `RewritePath=/api/workflow(?:/(?<segment>.*))?$, /api/${segment}`
- `custom-fields-service`: `RewritePath=/api/fields(?:/(?<segment>.*))?$, /api/${segment}`
- `notification-service`: pass-through (no filter — gateway path matches service path)
- `audit-service`: pass-through (no filter — gateway path matches service path)

**Also fixed:** Gateway env var approach changed from indexed (`SPRING_CLOUD_GATEWAY_MVC_ROUTES_N_URI`) to named (`WORKFLOW_SERVICE_URL`) to avoid partial route override issues.

**Runtime verified (2026-03-21):**
- Gateway → [[Workflow Service|workflow-service]] (`/api/workflow/deployments`): 200 OK
- Gateway → [[Custom Fields Service|custom-fields-service]] (`/api/fields/schemas?processDefinitionKey=test`): 200 OK
- Gateway → [[Notification Service|notification-service]] (`/api/notifications/unread-count`): 200 OK
- Gateway → [[Audit Service|audit-service]] (`/api/audit`): 200 OK

---


---

**Plan** — ← [[Step 02 — Runtime test custom-fields-service and notification-service]] · [[Step 04 — Docker full-stack build and E2E]] →

> [!abstract]- All notes in this set
> [[Active Tasks]]
> [[Project Context]]
> [[Step 01 — Fix Backend Dockerfiles]]
> [[Step 02 — Runtime test custom-fields-service and notification-service]]
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
