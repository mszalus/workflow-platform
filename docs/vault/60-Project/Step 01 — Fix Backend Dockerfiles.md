---
title: Step 01 — Fix Backend Dockerfiles
tags:
  - project
  - plan
  - status/done
type: plan
source: PLAN.md
status: done
---
[[Project MOC]] › **Step 01 — Fix Backend Dockerfiles**

4 service Dockerfiles used `../../` relative COPY paths which break with docker-compose `context: ..` (project root).

**Files fixed:**
- `services/workflow-service/Dockerfile`
- `services/custom-fields-service/Dockerfile`
- `services/notification-service/Dockerfile`
- `services/audit-service/Dockerfile`
- `services/gateway/Dockerfile` (also fixed: was missing `buildSrc/` and `gradle.properties`)

**Additional fixes discovered during build:**
- All Dockerfiles must `COPY services/ services/` (not just target service) because `settings.gradle` includes all modules and [[Build System|Gradle]] requires all project directories to exist
- Gateway Dockerfile switched to alpine images for consistency
- Gateway Dockerfile: `groupadd`/`useradd` → `addgroup`/`adduser` (alpine)

**Verified:** All 7 Docker images (5 backend + 2 frontend) build successfully.

---


---

**Plan** — ← [[Project Context]] · [[Step 02 — Runtime test custom-fields-service and notification-service]] →

> [!abstract]- All notes in this set
> [[Active Tasks]]
> [[Project Context]]
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
