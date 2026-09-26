---
title: Observability — 2 Distributed Tracing
tags:
  - ops
  - observability
type: report
source: observability-report.md
---
[[Operations MOC]] › [[Observability Stack]] › **Observability — 2 Distributed Tracing**

**Verified traces in [[Observability Stack|Grafana]] Tempo:**

```
Total traces: 5
  traceID=71c3de2c3d79c05e  svc=custom-fields-service  dur=2ms
  traceID=56cd34e6ac4496c2  svc=gateway                dur=1ms
  traceID=6656ca1a3b2a5889  svc=audit-service          dur=1ms
  traceID=90097961405445bc  svc=workflow-service        dur=2ms
  traceID=1caa3c7fc7943ef3  svc=custom-fields-service  dur=1ms
```

**Trace pipeline confirmed:**
1. Services generate spans via `micrometer-tracing-bridge-otel` (auto-instrumented Spring MVC)
2. Spans exported via OTLP HTTP to `http://otel-collector:4318`
3. OTEL Collector receives → batches → forwards to Tempo via OTLP gRPC
4. Tempo stores traces and serves queries on port 3200
5. Grafana has Tempo datasource auto-provisioned at `http://tempo:3200`

**Sampling:** `MANAGEMENT_TRACING_SAMPLING_PROBABILITY: 1.0` (100%) locally. GCP values set to 0.1 (10%).

**Cross-service trace propagation:** The gateway forwards the W3C `traceparent` header to downstream services. All services in a request share the same `traceId`.

---


---

**Observability report** — ← [[Observability — 1 Structured JSON Logging]] · [[Observability — 3 Prometheus Metrics]] →

> [!abstract]- All notes in this set
> [[Observability — Summary]]
> [[Observability — 1 Structured JSON Logging]]
> [[Observability — 3 Prometheus Metrics]]
> [[Observability — 4 Grafana]]
> [[Observability — 5 Full Observability Stack Verified]]
> [[Observability — 6 MDC Context Propagation]]
> [[Observability — Architecture for GCP]]
