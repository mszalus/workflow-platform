---
title: Active Tasks
tags:
  - project
  - plan
  - status/open
type: plan
source: PLAN.md
status: open
---
[[Project MOC]] › **Active Tasks**

Live cross-session task tracker. Items are removed once verified done (completed work lives in git log and the step records below). Kept in sync with the in-session task list.

**Phase 0 — Fix CI on main** (found 2026-09-26: the first main CI run since April 7 ran the main-only jobs for the first time)
- [ ] **Tenant isolation BDD fails in CI only**: "Tenant A cannot see Tenant B's process instances", where tenant B's list contains tenant A's instance. Locally all 20 scenarios pass on the same backend code. `ProcessService.listInstances` filters by tenant correctly, so the difference is the environment: CI has empty volumes and a fresh [[Security and JWT|Keycloak]] realm import, while the local database has lived since March. Reproduce on a fresh stack (`docker compose down -v`, which wipes local data) and treat it as a possible tenant leak until explained.
- [ ] **Custom fields: GET field values returns 500 in CI only** ("Save and retrieve field values for a process instance"). Reproduce on the same fresh stack and capture the [[Custom Fields Service|custom-fields-service]] logs.
- [ ] **Gateway header spoofing check**: when a JWT has no `tenant_id` claim, `TenantHeaderFilter` passes the request on unchanged, so a client-supplied `X-Tenant-Id` may reach the services. Verify, then always strip the incoming header and reject tokens without the claim.
- [ ] **docker-build: the Trivy v0.60.0 install fails** (the release download is no longer available). Pin an available version or switch to `aquasecurity/trivy-action`.
- [ ] **User action: regenerate `CLAUDE_CODE_OAUTH_TOKEN`** (`claude setup-token`, then `gh secret set CLAUDE_CODE_OAUTH_TOKEN`). `claude-review` and `@claude` fail after 2s with $0 cost.

**Phase 1 — Frontend unit tests** (the April 10 tests were lost before they were committed)
- [ ] Add Vitest + React Testing Library + jsdom, with a `test` script in `shared-ui`, `bpmn-editor`, `user-portal` and `admin-portal`.
- [ ] `shared-ui`: tests for `apiClient` (token and tenant headers, error mapping) and `AuthProvider`.
- [ ] `bpmn-editor`: tests for `FlowablePropertiesProvider` ([[Flowable Engine|Flowable]] extension properties).
- [ ] `user-portal`: tests for the task inbox, task detail with dynamic custom-field forms, and start process. Pre-seed the QueryClient cache.
- [ ] `admin-portal`: tests for the process list, deploy and BPMN import/export, the custom field editor, and the audit log.
- [ ] Commit together with the pending `ci.yml` test and coverage step, with coverage paths covering all 4 packages. The pre-commit and pre-push Claude hooks already run `npm run test --workspaces`, so they pick the tests up automatically.

**Phase 2 — GCP observability readiness** → see [Step 19](#step-19-gcp-observability-readiness-no-deployment). No deployment.

**Phase 3 — Work item tracker on BPMN** → see [Step 20](#step-20-work-item-tracker-on-bpmn--not-started-design-agreed-2026-09-26). Next up: the codebase simplification review (input in 20.7), then the 20.a spike.


---

**Plan** — [[Project Context]] →

> [!abstract]- All notes in this set
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
