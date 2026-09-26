---
title: Observability — 1 Structured JSON Logging
tags:
  - ops
  - observability
type: report
source: observability-report.md
---
[[Operations MOC]] › [[Observability Stack]] › **Observability — 1 Structured JSON Logging**

All 5 backend services emit JSON-structured logs to stdout. Sample from gateway:

```json
{
  "time": "2026-04-09T20:28:19.267Z",
  "message": "Starting GatewayApplication v0.1.0-SNAPSHOT",
  "logger": "com.wfp.gateway.GatewayApplication",
  "thread": "main",
  "service": "gateway",
  "severity": "INFO"
}
```

**Fields verified:**
- `time` — ISO-8601 UTC timestamp (maps to GCP Cloud Logging `timestamp`)
- `message` — log message
- `logger` — Java class name
- `thread` — thread name
- `service` — application name (from `spring.application.name`)
- `severity` — GCP-compatible level string (INFO/WARNING/ERROR/DEBUG)
- `traceId` / `spanId` — present during request processing when Micrometer Tracing is active
- `tenantId` / `userId` — present during authenticated requests (set by TenantInterceptor)

**GCP Cloud Logging compatibility:** When `GOOGLE_CLOUD_PROJECT` env var is set (done in `values-gcp.yaml`), the custom `GcpLoggingJsonProvider` adds:
- `logging.googleapis.com/trace` → links log entries to Cloud Trace
- `logging.googleapis.com/spanId` → span identifier
- `logging.googleapis.com/traceSampled: true`

**Logback configuration:** `logback-spring.xml` in each service. Two profiles:
- Non-local (GCP): `LoggingEventCompositeJsonEncoder` → JSON stdout
- Local dev: Colored console with trace/tenant context in pattern

---


---

**Observability report** — ← [[Observability — Summary]] · [[Observability — 2 Distributed Tracing]] →

> [!abstract]- All notes in this set
> [[Observability — Summary]]
> [[Observability — 2 Distributed Tracing]]
> [[Observability — 3 Prometheus Metrics]]
> [[Observability — 4 Grafana]]
> [[Observability — 5 Full Observability Stack Verified]]
> [[Observability — 6 MDC Context Propagation]]
> [[Observability — Architecture for GCP]]
