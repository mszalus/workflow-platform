---
title: Step 11 — Comprehensive E2E Testing and Bug Fixes
tags:
  - project
  - plan
  - status/done
type: plan
source: PLAN.md
status: done
---
[[Project MOC]] › **Step 11 — Comprehensive E2E Testing and Bug Fixes**

Extensive API and UI testing with [[Testing Strategy|Playwright]] MCP and curl. Found and fixed multiple critical bugs.

**Bugs found and fixed:**

1. **[[Flowable Engine|Flowable]] initiator resolution (500 on POST /api/processes):**
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
   - Fix: Added FlowableExceptionHandler in [[Workflow Service|workflow-service]] (400/404), added handlers in GlobalExceptionHandler for MissingParam, IllegalArgument, HttpMediaType

**Comprehensive E2E test suite:** `e2e/comprehensive.test.ts` — 71 tests across 13 categories covering health checks, auth, process lifecycle, approval flow, comments, custom fields, notifications, audit trail, multi-tenant isolation, negative tests, BPMN import/export, [[Admin Portal|admin portal]] UI, and [[User Portal|user portal]] UI.

**CI:** All 4 jobs pass (backend-build, frontend-build, docker-build, helm-lint).

---


---

**Plan** — ← [[Frontend API Path Bug Fix]] · [[Verification Log]] →

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
> [[Frontend API Path Bug Fix]]
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
