---
title: Step 06 — Helm deployment
tags:
  - project
  - plan
  - status/blocked
type: plan
source: PLAN.md
status: blocked
---
[[Project MOC]] › **Step 06 — Helm deployment**

[[Helm and Kubernetes|Helm]] is not installed on this machine. Sub-chart lint passes in CI (GitHub Actions installs helm via `azure/setup-helm@v4`).

**TODO when helm is available:**
1. `helm dependency update helm/workflow-platform/`
2. `helm lint helm/workflow-platform/ -f helm/workflow-platform/values-local.yaml`
3. If minikube available: `helm install wfp helm/workflow-platform/ -f helm/workflow-platform/values-local.yaml`

---


---

**Plan** — ← [[Step 10 — BPMN Import Export]] · [[CI Pipeline Fixes]] →

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
> [[Commit Strategy]]
