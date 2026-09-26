---
title: Observability — 6 MDC Context Propagation
tags:
  - ops
  - observability
type: report
source: observability-report.md
---
[[Operations MOC]] › [[Observability Stack]] › **Observability — 6 MDC Context Propagation**

MDC propagation is configured in `TenantInterceptor` (calls `MDC.put("tenantId"...)` and `MDC.put("userId",...)`) and Micrometer Tracing auto-populates `traceId` and `spanId`. These fields appear in any log statement emitted during request handling.

To verify in a running system:
1. Enable DEBUG logging for `com.wfp` temporarily
2. Make an authenticated request
3. All log entries during that request will include `traceId`, `tenantId`, `userId`

This is automatically configured for the `local` Spring profile in `logback-spring.xml`.

---


---

**Observability report** — ← [[Observability — 5 Full Observability Stack Verified]] · [[Observability — Architecture for GCP]] →

> [!abstract]- All notes in this set
> [[Observability — Summary]]
> [[Observability — 1 Structured JSON Logging]]
> [[Observability — 2 Distributed Tracing]]
> [[Observability — 3 Prometheus Metrics]]
> [[Observability — 4 Grafana]]
> [[Observability — 5 Full Observability Stack Verified]]
> [[Observability — Architecture for GCP]]
