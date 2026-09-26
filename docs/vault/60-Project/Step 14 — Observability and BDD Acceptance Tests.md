---
title: Step 14 — Observability and BDD Acceptance Tests
tags:
  - project
  - plan
  - status/done
type: plan
source: PLAN.md
status: done
---
[[Project MOC]] › **Step 14 — Observability and BDD Acceptance Tests**

**Observability (commits 45349b8, 02052a6):**

Structured JSON logging, distributed tracing via OpenTelemetry, and [[Observability Stack|Prometheus]] metrics added to all five services. Ready for GCP Cloud Logging, Cloud Trace, and Google Managed Prometheus without code changes — only env-var overrides.

- `logstash-logback-encoder` + `logback-spring.xml` on all services: JSON to stdout (`!local` profile), colored console for `local` profile
- `GcpLoggingJsonProvider` (wfp-common): maps WARN→WARNING for GCP severity; adds `logging.googleapis.com/trace` + span fields when `GOOGLE_CLOUD_PROJECT` is set
- `TenantInterceptor` writes `tenantId` and `userId` to MDC on every request
- `micrometer-registry-prometheus`: `/actuator/prometheus` on all services
- `micrometer-tracing-bridge-otel` + `opentelemetry-exporter-otlp`: traces sent to `OTEL_EXPORTER_OTLP_ENDPOINT` (default `http://localhost:4318`)
- Local stack added to docker-compose: OTEL Collector (4317/4318) → Tempo → Grafana (3000); Prometheus (9090) → Grafana; both datasources auto-provisioned
- [[Helm and Kubernetes|Helm]] deployment templates: `prometheus.io/scrape` annotations on all pods; `OTEL_EXPORTER_OTLP_ENDPOINT` and `MANAGEMENT_TRACING_SAMPLING_PROBABILITY=0.1` in per-service values
- `values-gcp.yaml`: documents Cloud Trace (OTLP collector swap), GMP (annotations already present), Cloud Logging (set `GOOGLE_CLOUD_PROJECT`)

**BDD Acceptance Tests (commits 1f4f822, 30de72d, 6cc3f4e, 0f1cc94):**

20 Cucumber scenarios across 4 phases, all passing:
- Phase A: process management + task lifecycle (8 scenarios)
- Phase B: [[Multi-Tenancy|multi-tenancy]] isolation, audit trail, API security (8 scenarios)
- Phase C: custom fields, notifications via [[Event System|RabbitMQ]] (4 scenarios)
- Phase D: `acceptance-tests` CI job (builds images, starts stack, runs BDD, uploads report)

---


---

**Plan** — ← [[Step 13 — SDLC Improvements]] · [[Step 15 — Local Observability Verification]] →

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
> [[Step 15 — Local Observability Verification]]
> [[Step 16 — GCP Infrastructure Terraform Helm Preparation]]
> [[Step 17 — GCP Deployment and Acceptance Testing]]
> [[GCP Cost Estimate]]
> [[Step 18 — Release and Rollback Strategy]]
> [[Step 19 — GCP Observability Readiness no deployment]]
> [[Step 20 — Work Item Tracker on BPMN]]
> [[Commit Strategy]]
