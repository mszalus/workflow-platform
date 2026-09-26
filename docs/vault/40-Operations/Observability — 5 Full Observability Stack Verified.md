---
title: Observability — 5 Full Observability Stack Verified
tags:
  - ops
  - observability
type: report
source: observability-report.md
---
[[Operations MOC]] › [[Observability Stack]] › **Observability — 5 Full Observability Stack Verified**

All components confirmed working locally on 2026-04-09:

```
✅ JSON structured logs    — all 5 services, GCP-severity-compatible format
✅ Distributed traces      — 5 traces in Tempo from gateway, workflow, audit, custom-fields
✅ Prometheus metrics      — all 5 services reporting JVM + HTTP metrics
✅ Grafana (port 3000)     — Prometheus + Tempo datasources auto-provisioned
✅ Grafana Tempo (3200)    — traces stored, searchable by service name
✅ OTEL Collector (4317/4318) — receiving OTLP spans, forwarding to Tempo
```

---


---

**Observability report** — ← [[Observability — 4 Grafana]] · [[Observability — 6 MDC Context Propagation]] →

> [!abstract]- All notes in this set
> [[Observability — Summary]]
> [[Observability — 1 Structured JSON Logging]]
> [[Observability — 2 Distributed Tracing]]
> [[Observability — 3 Prometheus Metrics]]
> [[Observability — 4 Grafana]]
> [[Observability — 6 MDC Context Propagation]]
> [[Observability — Architecture for GCP]]
