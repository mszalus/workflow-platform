---
title: Observability — 4 Grafana
tags:
  - ops
  - observability
type: report
source: observability-report.md
---
[[Operations MOC]] › [[Observability Stack]] › **Observability — 4 Grafana**

**Running at:** http://localhost:3000 (admin/admin, anonymous access enabled)

**Auto-provisioned datasources:**
- [[Observability Stack|Prometheus]] → `http://prometheus:9090` (default)
- Tempo → `http://tempo:3200`

Trace-to-metrics correlation works once Prometheus metrics are available: in Tempo trace details, clicking a service name opens the corresponding Grafana dashboard.

---


---

**Observability report** — ← [[Observability — 3 Prometheus Metrics]] · [[Observability — 5 Full Observability Stack Verified]] →

> [!abstract]- All notes in this set
> [[Observability — Summary]]
> [[Observability — 1 Structured JSON Logging]]
> [[Observability — 2 Distributed Tracing]]
> [[Observability — 3 Prometheus Metrics]]
> [[Observability — 5 Full Observability Stack Verified]]
> [[Observability — 6 MDC Context Propagation]]
> [[Observability — Architecture for GCP]]
