---
title: Step 07 — Playwright E2E Tests
tags:
  - project
  - plan
  - status/done
type: plan
source: PLAN.md
status: done
---
[[Project MOC]] › **Step 07 — Playwright E2E Tests**

[[Testing Strategy|Playwright]] test suite in `e2e/` directory. All 8 tests pass against live Docker stack.

**Files:**
- `e2e/playwright.config.ts` — config (baseURL: localhost:9080, 30s timeout)
- `e2e/playwright.test.ts` — 8 test cases
- `e2e/package.json` — standalone package with `@playwright/test`

**Test results (2026-03-21, all pass):**
1. [[Security and JWT|Keycloak]] realm exists — OIDC discovery endpoint returns issuer
2. [[Event System|RabbitMQ]] management accessible — login + Overview page
3. [[Admin Portal]] loads and redirects to Keycloak — OIDC redirect with correct client_id
4. [[User Portal]] loads and redirects to Keycloak — OIDC redirect with correct client_id
5. Gateway health check — actuator/health returns UP
6. All backend services healthy — ports 8081-8084 all UP
7. Full workflow E2E through gateway — deploy → start → list tasks → complete → audit → notifications
8. Multi-tenant isolation — tenant-a and tenant-b see only their own processes

**Fixes applied to make tests pass:**
- RabbitMQ login: switched from `#username`/`#password` CSS selectors to role-based `getByRole('textbox')` (RabbitMQ management UI doesn't use ID attributes)
- Frontend portals: OIDC-protected apps redirect to Keycloak, so tests verify the redirect URL and Keycloak login page instead of checking for `#root`
- RabbitMQ Overview assertion: used `getByRole('heading')` to disambiguate from nav link

---


---

**Plan** — ← [[CI Pipeline Fixes]] · [[Step 08 — Flowable BPMN Editor Extensions]] →

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
