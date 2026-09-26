---
title: Step 19 — GCP Observability Readiness no deployment
tags:
  - project
  - plan
  - status/open
type: plan
source: PLAN.md
status: open
---
[[Project MOC]] › **Step 19 — GCP Observability Readiness no deployment**

**Goal:** Close the gaps between the local observability stack from step 14/15 and what GKE, Cloud Logging, Cloud Trace and Managed [[Observability Stack|Prometheus]] actually need. Everything is verified locally. The GCP deployment (step 17) stays parked.

| # | Gap found (2026-09-26) | Change |
|---|---|---|
| 19.1 | Gateway serves `/actuator/prometheus` with `permitAll`, so it would be public through the GKE ingress | Move actuator to a separate `management.server.port` on every service, drop the `permitAll`, and keep the port out of the ingress and service |
| 19.2 | Liveness and readiness probes both hit `/actuator/health`, so a DB or [[Event System|RabbitMQ]] outage restarts every pod | Enable probe groups: liveness → `/actuator/health/liveness` (no dependencies), readiness → `/actuator/health/readiness` (DB, RabbitMQ) |
| 19.3 | GMP ignores `prometheus.io/scrape` annotations, which contradicts `values-gcp.yaml` and the report | Add `PodMonitoring` resources to the [[Helm and Kubernetes|Helm]] charts, enabled by a values flag in `values-gcp.yaml`, and correct the docs |
| 19.4 | Traces stop at RabbitMQ, so audit and notification consumers start new traces | Enable Micrometer observation on `RabbitTemplate` and the listener container factory, then check in Tempo for one trace spanning gateway → workflow → audit/notification |
| 19.5 | Only JVM/HTTP metrics exist; there are no business metrics | Counters/timers: processes started and completed, tasks completed plus duration, events published/consumed/failed, notifications created. The `tenant` tag sits behind the `wfp.metrics.tenant-tag-enabled` setting, **default `false`**, to control Managed Prometheus cost |
| 19.6 | Log/trace details | `traceSampled` should reflect the real sampling decision instead of always `true`. ERROR entries should be Error Reporting-compatible (`serviceContext`, stack trace). Add the OTel resource attributes `service.version` and `deployment.environment` |
| 19.7 | No dashboards or alerts | Grafana dashboard JSON (RED per service, business metrics, queue depth) provisioned locally. Define the SLOs (availability, p95 latency, DLQ depth) that later become Cloud Monitoring alerts |
| 19.8 | Verification | Re-run the step 15 checklist plus the new items, update `observability-report.md`, and regenerate the vault |

**Out of scope:** frontend RUM/tracing, Cloud Monitoring alert Terraform (after 19.7), and the actual GKE deployment.

**Verification checklist:**
- [ ] `curl gateway:<public-port>/actuator/prometheus` → 404/401; the management port serves the metrics
- [ ] Stopping RabbitMQ makes readiness fail while liveness stays UP
- [ ] `helm template -f values-gcp.yaml` renders `PodMonitoring` for all 5 services
- [ ] One Tempo trace covers the HTTP request and the async RabbitMQ consumers
- [ ] Business metrics are visible in Prometheus after a BDD run; all 20 BDD scenarios pass

---


---

**Plan** — ← [[Step 18 — Release and Rollback Strategy]] · [[Step 20 — Work Item Tracker on BPMN]] →

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
> [[Step 13 — SDLC Improvements]]
> [[Step 14 — Observability and BDD Acceptance Tests]]
> [[Step 15 — Local Observability Verification]]
> [[Step 16 — GCP Infrastructure Terraform Helm Preparation]]
> [[Step 17 — GCP Deployment and Acceptance Testing]]
> [[GCP Cost Estimate]]
> [[Step 18 — Release and Rollback Strategy]]
> [[Step 20 — Work Item Tracker on BPMN]]
> [[Commit Strategy]]
