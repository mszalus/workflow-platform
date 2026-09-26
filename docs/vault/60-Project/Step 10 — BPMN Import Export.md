---
title: Step 10 — BPMN Import Export
tags:
  - project
  - plan
  - status/done
type: plan
source: PLAN.md
status: done
---
[[Project MOC]] › **Step 10 — BPMN Import Export**

Added upload (import), download (export), and edit-existing functionality to the Process Designer.

**Backend:**
- `DeploymentController.java` — added `GET /api/deployments/{processDefinitionId}/bpmn` endpoint
- `DeploymentService.java` — added `getProcessDefinitionBpmnXml()` method using [[Flowable Engine|Flowable]] `RepositoryService.getResourceAsStream()`

**Frontend:**
- `ProcessDesigner.tsx` — complete rewrite with Import (file upload via FileReader), Export (Blob download), and Edit (load existing BPMN from Flowable when URL has `:id` param). Auto-fills process name from filename on import.
- `ProcessList.tsx` — added Edit link for each process definition, linking to `/processes/designer/:id`

**Round-trip verification (all PASS):**
1. Deploy BPMN with 6 Flowable properties via curl → retrieve XML → all properties preserved
2. Re-deploy retrieved XML → retrieve again → all properties still intact
3. Browser: open existing process via Edit → Flowable properties panel shows all values correctly (assignee, candidateGroups, formKey, priority, async, class)
4. Browser: export XML from editor → all `flowable:*` attributes present in output
5. Browser: import exported file into fresh designer → all Flowable properties visible in panel

---


---

**Plan** — ← [[Step 09 — Documentation User Manual and Admin Manual]] · [[Step 06 — Helm deployment]] →

> [!abstract]- All notes in this set
> [[Active Tasks]]
> [[Project Context]]
> [[Step 01 — Fix Backend Dockerfiles]]
> [[Step 02 — Runtime test custom-fields-service and notification-service]]
> [[Step 03 — Gateway routing test]]
> [[Step 04 — Docker full-stack build and E2E]]
> [[Step 05 — README documentation]]
> [[Step 09 — Documentation User Manual and Admin Manual]]
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
> [[Commit Strategy]]
