---
title: Step 12 — Architecture Diagrams and Data Model
tags:
  - project
  - plan
  - status/done
type: plan
source: PLAN.md
status: done
---
[[Project MOC]] › **Step 12 — Architecture Diagrams and Data Model**

Created C4 model diagrams and data model documentation in `docs/architecture/` using Mermaid format (editable, version-controlled, GitHub-rendered).

**Files created:**
- `docs/architecture/README.md` — Index of all diagrams with viewing/editing instructions
- `docs/architecture/c4-context.md` — C4 Level 1: System Context (users, external systems, platform boundary)
- `docs/architecture/c4-container.md` — C4 Level 2: All containers (10 services, DB, MQ, gateway routing table, event flows)
- `docs/architecture/c4-component-workflow-service.md` — C4 Level 3: Workflow service internals (controllers, services, [[Flowable Engine|Flowable engine]], event publisher)
- `docs/architecture/c4-component-notification-service.md` — C4 Level 3: Notification service (event listener, CRUD, event-to-notification mapping)
- `docs/architecture/c4-component-gateway.md` — C4 Level 3: Gateway (JWT validation, tenant propagation, routing)
- `docs/architecture/c4-deployment.md` — C4 Level 4: Docker Compose topology, GCP VM deployment, Kubernetes/Helm with resource allocation
- `docs/architecture/data-model.md` — ER diagram: 9 JPA entities + key Flowable tables, enumerations, cross-schema references, [[Multi-Tenancy|multi-tenancy]] pattern

Each diagram includes a "Notes for Editors" section explaining how to extend it for common changes (add service, add entity, add route, etc.).

---


---

**Plan** — ← [[Verification Log]] · [[Step 13 — SDLC Improvements]] →

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
> [[Step 11 — Comprehensive E2E Testing and Bug Fixes]]
> [[Verification Log]]
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
