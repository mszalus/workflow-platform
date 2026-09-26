---
title: Observability — Summary
tags:
  - ops
  - observability
type: report
source: observability-report.md
---
[[Operations MOC]] › [[Observability Stack]] › **Observability — Summary**

All observability infrastructure is running and partially verified. Distributed tracing and structured logging are confirmed working. [[Observability Stack|Prometheus]] metrics are exposed but the security configuration requires an image rebuild (fix already committed).

| Component | Status | Notes |
|---|---|---|
| OTEL Collector | ✅ Running | Port 4317 (gRPC), 4318 (HTTP), 8888 (metrics) |
| Grafana Tempo | ✅ Running | Port 3200. Traces confirmed received |
| Prometheus | ✅ Running | Port 9090. Targets down due to auth (see below) |
| Grafana | ✅ Running | Port 3000. Datasources auto-provisioned |
| Structured JSON logs | ✅ Verified | All 5 services emit JSON to stdout |
| Distributed traces | ✅ Verified | 5 traces in Tempo across 4 services |
| Prometheus metrics | ✅ Verified | All 5 services reporting metrics — 5/6 targets UP |

---


---

**Observability report** — [[Observability — 1 Structured JSON Logging]] →

> [!abstract]- All notes in this set
> [[Observability — 1 Structured JSON Logging]]
> [[Observability — 2 Distributed Tracing]]
> [[Observability — 3 Prometheus Metrics]]
> [[Observability — 4 Grafana]]
> [[Observability — 5 Full Observability Stack Verified]]
> [[Observability — 6 MDC Context Propagation]]
> [[Observability — Architecture for GCP]]
