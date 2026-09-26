---
title: CI Pipeline Fixes
tags:
  - project
  - plan
  - status/done
type: plan
source: PLAN.md
status: done
---
[[Project MOC]] › **CI Pipeline Fixes**

Two issues causing CI failures on every push to `main`:

1. **`gradlew` not executable** (exit code 126): File was committed with `100644` mode. Fix: `git update-index --chmod=+x gradlew` → now `100755`.
2. **Frontend typecheck fails in CI** (clean checkout has no `dist/`): Packages used `composite: true` with project references, requiring built `.d.ts` output that doesn't exist in CI. Fix: removed `composite`/`declaration`/`declarationMap` from [[shared-ui]] and bpmn-editor tsconfigs, removed `references` from [[Admin Portal|admin-portal]] and [[User Portal|user-portal]] tsconfigs. Vite resolves imports from source via npm workspace symlinks — no build artifacts needed.

**Verified:** `npm run typecheck --workspaces --if-present` passes with `dist/` directories deleted.

---


---

**Plan** — ← [[Step 06 — Helm deployment]] · [[Step 07 — Playwright E2E Tests]] →

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
