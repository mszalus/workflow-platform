---
title: Project MOC
tags:
  - moc
  - project
type: moc
source: PLAN.md
---

Execution history and forward plan, split out of `PLAN.md`.

[[Active Tasks]] · [[Project Context]] · [[Verification Log]] · [[Commit Strategy]]

## Delivered

[[Step 01 — Fix Backend Dockerfiles]] · [[Step 02 — Runtime test custom-fields-service and notification-service]]
[[Step 03 — Gateway routing test]] · [[Step 04 — Docker full-stack build and E2E]]
[[Step 05 — README documentation]] · [[Step 07 — Playwright E2E Tests]]
[[Step 08 — Flowable BPMN Editor Extensions]] · [[Step 09 — Documentation User Manual and Admin Manual]]
[[Step 10 — BPMN Import Export]] · [[Step 11 — Comprehensive E2E Testing and Bug Fixes]]
[[Step 12 — Architecture Diagrams and Data Model]] · [[Step 13 — SDLC Improvements]]
[[Step 14 — Observability and BDD Acceptance Tests]]
[[CI Pipeline Fixes]] · [[Frontend API Path Bug Fix]]

## Blocked

[[Step 06 — Helm deployment]] — no `helm` binary available locally

## Open

[[Step 15 — Local Observability Verification]] · [[Step 16 — GCP Infrastructure Terraform Helm Preparation]]
[[Step 17 — GCP Deployment and Acceptance Testing]] · [[Step 18 — Release and Rollback Strategy]]
[[Step 19 — GCP Observability Readiness no deployment]] · [[Step 20 — Work Item Tracker on BPMN]]
[[GCP Cost Estimate]]

## Known gaps in the product

| Gap | Where |
|---|---|
| `Attachment` has no REST endpoint | [[Attachment]] |
| `NotificationPreference` is never consulted | [[NotificationPreference]] |
| No email delivery channel | [[Notification Service]] |
| `process.sla.breached` has no publisher | [[Event Catalog]] |
| `field.*` events declared but unused | [[Event Catalog]] |
| No frontend unit test framework | [[Testing Strategy]] |
