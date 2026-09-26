---
title: Step 13 — SDLC Improvements
tags:
  - project
  - plan
  - status/done
type: plan
source: PLAN.md
status: done
---
[[Project MOC]] › **Step 13 — SDLC Improvements**

All 9 phases of the SDLC improvements plan implemented and committed.

**Phases 1–8 (previously committed):**
- Phase 1: ESLint 9 (flat config) + Prettier 3 — frontend linting and formatting
- Phase 2: Checkstyle 10 — Java style enforcement across all modules
- Phase 3: Husky + lint-staged — pre-commit hooks
- Phase 4: Commitlint — conventional commit message enforcement
- Phase 5a: OWASP dependency-check + npm audit — security scanning
- Phase 5b: Trivy — container image CVE scanning in CI
- Phase 6: Docker hardening — all containers run as non-root (appuser / nginx-unprivileged)
- Phase 7: JaCoCo — test coverage reporting (XML + HTML)
- Phase 8: CSRF comment in SecurityConfig, branch protection documented

**Phase 9 (committed 2026-04-06, 5 commits):**
- Removed all Checkstyle suppressions one service at a time
- Fixed every violation: star imports → explicit imports, LeftCurly, NeedBraces, unused imports, long lines
- Deleted `config/checkstyle/suppressions.xml` and removed SuppressionFilter from `checkstyle.xml`
- All 30 `checkstyleMain` tasks pass with zero violations

---


---

**Plan** — ← [[Step 12 — Architecture Diagrams and Data Model]] · [[Step 14 — Observability and BDD Acceptance Tests]] →

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
> [[Step 12 — Architecture Diagrams and Data Model]]
> [[Step 14 — Observability and BDD Acceptance Tests]]
> [[Step 15 — Local Observability Verification]]
> [[Step 16 — GCP Infrastructure Terraform Helm Preparation]]
> [[Step 17 — GCP Deployment and Acceptance Testing]]
> [[GCP Cost Estimate]]
> [[Step 18 — Release and Rollback Strategy]]
> [[Step 19 — GCP Observability Readiness no deployment]]
> [[Commit Strategy]]
