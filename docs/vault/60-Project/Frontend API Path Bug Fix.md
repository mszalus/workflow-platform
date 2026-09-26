---
title: Frontend API Path Bug Fix
tags:
  - project
  - plan
  - status/done
type: plan
source: PLAN.md
status: done
---
[[Project MOC]] › **Frontend API Path Bug Fix**

**Bug:** All frontend API calls used doubled `/api` prefix. The axios client has `baseURL: '/api'`, but every call also included `/api/` in the path (e.g., `apiClient.get('/api/workflow/deployments')` → request to `/api/api/workflow/deployments`).

**Additional issue:** Notification and audit paths were also doubled at the service level: `/api/notifications/notifications` and `/api/audit/audit`.

**Fix:** Removed `/api` prefix from all 22 API calls across 11 frontend files. Paths now use relative service paths (e.g., `/workflow/deployments`, `/notifications`, `/audit`).

**Files fixed ([[Admin Portal|admin-portal]]):**
- `Dashboard.tsx`, `ProcessList.tsx`, `ProcessDesigner.tsx`, `CustomFieldEditor.tsx`, `AuditLog.tsx`

**Files fixed ([[User Portal|user-portal]]):**
- `Dashboard.tsx`, `TaskInbox.tsx`, `TaskDetail.tsx`, `StartProcess.tsx`, `MyProcesses.tsx`, `Notifications.tsx`, `DynamicFieldForm.tsx`

**Verified:**
- TypeScript typecheck passes
- Docker frontend images rebuilt and restarted
- Nginx proxy paths verified via curl: admin-portal:5173/api/* and user-portal:5174/api/* → gateway → backend services (all 200 OK)
- All 8 [[Testing Strategy|Playwright]] E2E tests pass

---


---

**Plan** — ← [[Step 08 — Flowable BPMN Editor Extensions]] · [[Step 11 — Comprehensive E2E Testing and Bug Fixes]] →

> [!abstract]- All notes in this set
> [[Active Tasks]]
> [[Project Context]]
> [[Step 01 — Fix Backend Dockerfiles]]
> [[Step 02 — Runtime test custom-fields-service and notification-service]]
> [[Step 03 — Gateway routing test]]
> [[Step 04 — Docker full-stack build and E2E]]
> [[Step 05 — README documentation]]
> [[Step 09 — Documentation User Manual and Admin Manual]]
> [[Step 10 — BPMN Import Export]]
> [[Step 06 — Helm deployment]]
> [[CI Pipeline Fixes]]
> [[Step 07 — Playwright E2E Tests]]
> [[Step 08 — Flowable BPMN Editor Extensions]]
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
