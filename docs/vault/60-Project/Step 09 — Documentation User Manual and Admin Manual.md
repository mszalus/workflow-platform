---
title: Step 09 — Documentation User Manual and Admin Manual
tags:
  - project
  - plan
  - status/done
type: plan
source: PLAN.md
status: done
---
[[Project MOC]] › **Step 09 — Documentation User Manual and Admin Manual**

**User Manual** (`docs/user-manual.md`):
- Getting started, login flow ([[Security and JWT|Keycloak]] OIDC), dashboard
- Task inbox: viewing, opening, completing tasks with custom fields and comments
- Starting a new process, tracking process instances
- Notifications: viewing, marking as read
- Multi-tenant isolation explanation, troubleshooting guide

**Admin Manual** (`docs/admin-manual.md`):
- Architecture diagram with gateway routing table
- Process Designer: BPMN editor with [[Flowable Engine|Flowable]] properties panel (user task, service task, async)
- Process definition management (deploy, delete)
- Custom field schemas: creating, deleting, field types
- Audit log: filtering by type/user, pagination, event types
- Keycloak administration: realm structure, user management, [[Multi-Tenancy|tenant isolation]], JWT claims
- [[Event System|RabbitMQ]] monitoring: exchanges, queues, troubleshooting
- Docker deployment: container overview, startup order, environment variables, database schemas
- Kubernetes/Helm deployment: chart structure, install commands
- Comprehensive troubleshooting table and useful curl commands

**Note:** Screenshots not included ([[Testing Strategy|Playwright]] MCP browser launch conflicts with existing Chrome session). Keycloak login screenshot captured at `docs/screenshots/keycloak-login.png`.

---


---

**Plan** — ← [[Step 05 — README documentation]] · [[Step 10 — BPMN Import Export]] →

> [!abstract]- All notes in this set
> [[Active Tasks]]
> [[Project Context]]
> [[Step 01 — Fix Backend Dockerfiles]]
> [[Step 02 — Runtime test custom-fields-service and notification-service]]
> [[Step 03 — Gateway routing test]]
> [[Step 04 — Docker full-stack build and E2E]]
> [[Step 05 — README documentation]]
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
