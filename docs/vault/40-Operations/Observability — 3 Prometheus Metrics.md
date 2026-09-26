---
title: Observability — 3 Prometheus Metrics
tags:
  - ops
  - observability
type: report
source: observability-report.md
---
[[Operations MOC]] › [[Observability Stack]] › **Observability — 3 Prometheus Metrics**

**Verified targets (5/6 UP):**

```
up   audit-service           (port 8084)
up   custom-fields-service   (port 8082)
up   gateway                 (port 8080)
up   notification-service    (port 8083)
down otel-collector          (port 8888 — uses /metrics, different format)
up   workflow-service        (port 8081)
```

**JVM thread metrics confirmed:**
```
gateway:                29 threads
workflow-service:       32 threads
custom-fields-service:  29 threads
notification-service:   33 threads
audit-service:          33 threads
```

**Fix required:** `SecurityConfig` in `wfp-security` only permits `/actuator/health` and `/actuator/info`. The gateway additionally has `GatewaySecurityConfig` which was also missing `/actuator/prometheus`. Both were fixed:
- `libs/wfp-security/src/main/java/com/wfp/security/config/SecurityConfig.java`
- `services/gateway/src/main/java/com/wfp/gateway/config/GatewaySecurityConfig.java`

**[[Observability Stack|Prometheus]] configuration:** `docker/prometheus.yml` scrapes all 5 services + otel-collector every 15 seconds.

**Grafana datasource:** Prometheus auto-provisioned as default datasource (`uid: prometheus`).

**GCP:** Google Managed Prometheus (GMP) auto-discovers pods via `prometheus.io/scrape: "true"` annotations (already set in all [[Helm and Kubernetes|Helm]] charts). No additional config needed on GCP.

---


---

**Observability report** — ← [[Observability — 2 Distributed Tracing]] · [[Observability — 4 Grafana]] →

> [!abstract]- All notes in this set
> [[Observability — Summary]]
> [[Observability — 1 Structured JSON Logging]]
> [[Observability — 2 Distributed Tracing]]
> [[Observability — 4 Grafana]]
> [[Observability — 5 Full Observability Stack Verified]]
> [[Observability — 6 MDC Context Propagation]]
> [[Observability — Architecture for GCP]]
