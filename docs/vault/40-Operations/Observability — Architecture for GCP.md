---
title: Observability — Architecture for GCP
tags:
  - ops
  - observability
type: report
source: observability-report.md
---
[[Operations MOC]] › [[Observability Stack]] › **Observability — Architecture for GCP**

```
Browser → Cloud Load Balancer → GKE Gateway Pod
           ↓ logs (stdout JSON)      ↓ OTEL spans
    Cloud Logging            OTEL Collector Pod
    (auto-collected              ↓
     from stdout)        Cloud Trace (googlecloud exporter)
    
GKE Pod metrics → Google Managed Prometheus (via prometheus.io/scrape annotations)
               → Cloud Monitoring dashboards
```


---

**Observability report** — ← [[Observability — 6 MDC Context Propagation]]

> [!abstract]- All notes in this set
> [[Observability — Summary]]
> [[Observability — 1 Structured JSON Logging]]
> [[Observability — 2 Distributed Tracing]]
> [[Observability — 3 Prometheus Metrics]]
> [[Observability — 4 Grafana]]
> [[Observability — 5 Full Observability Stack Verified]]
> [[Observability — 6 MDC Context Propagation]]
