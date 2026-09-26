---
title: Step 04 — Docker full-stack build and E2E
tags:
  - project
  - plan
  - status/done
type: plan
source: PLAN.md
status: done
---
[[Project MOC]] › **Step 04 — Docker full-stack build and E2E**

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
- Audit trail captured all events: [[process.started]], [[task.assigned]], [[task.created]], [[task.completed]] (9 total entries)

---


---

**Plan** — ← [[Step 03 — Gateway routing test]] · [[Step 05 — README documentation]] →

> [!abstract]- All notes in this set
> [[Active Tasks]]
> [[Project Context]]
> [[Step 01 — Fix Backend Dockerfiles]]
> [[Step 02 — Runtime test custom-fields-service and notification-service]]
> [[Step 03 — Gateway routing test]]
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
