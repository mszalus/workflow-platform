---
title: Step 08 — Flowable BPMN Editor Extensions
tags:
  - project
  - plan
  - status/done
type: plan
source: PLAN.md
status: done
---
[[Project MOC]] › **Step 08 — Flowable BPMN Editor Extensions**

Added native [[Flowable Engine|Flowable]] support to [[bpmn-editor|bpmn-js]] editor. Produces `flowable:*` XML attributes directly.

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
- `frontend/apps/admin-portal/src/env.d.ts` — ambient module declarations for [[Admin Portal|admin-portal]] TypeScript

**Verification:** `npm run typecheck --workspaces --if-present` passes. Runtime verification pending (needs frontend dev server or Docker rebuild).

---


---

**Plan** — ← [[Step 07 — Playwright E2E Tests]] · [[Frontend API Path Bug Fix]] →

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
> [[Commit Strategy]]
