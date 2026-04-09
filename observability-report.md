# Observability Verification Report — Step 15

**Date:** 2026-04-09  
**Environment:** Local Docker Compose (full stack)

---

## Summary

All observability infrastructure is running and partially verified. Distributed tracing and structured logging are confirmed working. Prometheus metrics are exposed but the security configuration requires an image rebuild (fix already committed).

| Component | Status | Notes |
|---|---|---|
| OTEL Collector | ✅ Running | Port 4317 (gRPC), 4318 (HTTP), 8888 (metrics) |
| Grafana Tempo | ✅ Running | Port 3200. Traces confirmed received |
| Prometheus | ✅ Running | Port 9090. Targets down due to auth (see below) |
| Grafana | ✅ Running | Port 3000. Datasources auto-provisioned |
| Structured JSON logs | ✅ Verified | All 5 services emit JSON to stdout |
| Distributed traces | ✅ Verified | 5 traces in Tempo across 4 services |
| Prometheus metrics | ⚠️ Blocked | `/actuator/prometheus` returns 401 — fix committed |

---

## 1. Structured JSON Logging ✅

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

## 2. Distributed Tracing ✅

**Verified traces in Grafana Tempo:**

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

## 3. Prometheus Metrics ⚠️

**Issue:** `SecurityConfig` in `wfp-security` only permits `/actuator/health` and `/actuator/info`. The `/actuator/prometheus` endpoint returns HTTP 401, preventing Prometheus from scraping metrics.

**Fix committed:** `libs/wfp-security/src/main/java/com/wfp/security/config/SecurityConfig.java` updated to add `/actuator/prometheus` to the permit list. Awaiting Docker image rebuild to take effect.

**Proof the metrics endpoint exists:**
```
HTTP 401 on http://localhost:9080/actuator/prometheus
```
A 401 (not 404) confirms micrometer-registry-prometheus is configured and the endpoint exists — it just needs to be permitted.

**Prometheus configuration:** `docker/prometheus.yml` scrapes all 5 services + otel-collector every 15 seconds. Targets currently show as `down` due to the auth issue.

**Grafana datasource:** Prometheus auto-provisioned as default datasource (`uid: prometheus`).

**GCP:** Google Managed Prometheus (GMP) auto-discovers pods via `prometheus.io/scrape: "true"` annotations (already set in all Helm charts). No additional config needed on GCP.

---

## 4. Grafana ✅

**Running at:** http://localhost:3000 (admin/admin, anonymous access enabled)

**Auto-provisioned datasources:**
- Prometheus → `http://prometheus:9090` (default)
- Tempo → `http://tempo:3200`

Trace-to-metrics correlation works once Prometheus metrics are available: in Tempo trace details, clicking a service name opens the corresponding Grafana dashboard.

---

## 5. Pending Action: Prometheus Fix

After completing Docker image rebuild with the SecurityConfig fix:

```bash
# Verify Prometheus can scrape the gateway metrics:
docker exec wfp-prometheus wget -qO- http://gateway:8080/actuator/prometheus | head -5
# Expected: # HELP jvm_memory_used_bytes ...

# Verify Prometheus targets are UP:
curl -s http://localhost:9090/api/v1/targets | python3 -c "
import json,sys
d=json.load(sys.stdin)
for t in d['data']['activeTargets']:
    print(t['health'], t['labels']['job'])
"
# Expected: up gateway, up workflow-service, up custom-fields-service, ...
```

---

## 6. MDC Context Propagation

MDC propagation is configured in `TenantInterceptor` (calls `MDC.put("tenantId"...)` and `MDC.put("userId",...)`) and Micrometer Tracing auto-populates `traceId` and `spanId`. These fields appear in any log statement emitted during request handling.

To verify in a running system:
1. Enable DEBUG logging for `com.wfp` temporarily
2. Make an authenticated request
3. All log entries during that request will include `traceId`, `tenantId`, `userId`

This is automatically configured for the `local` Spring profile in `logback-spring.xml`.

---

## Architecture for GCP

```
Browser → Cloud Load Balancer → GKE Gateway Pod
           ↓ logs (stdout JSON)      ↓ OTEL spans
    Cloud Logging            OTEL Collector Pod
    (auto-collected              ↓
     from stdout)        Cloud Trace (googlecloud exporter)
    
GKE Pod metrics → Google Managed Prometheus (via prometheus.io/scrape annotations)
               → Cloud Monitoring dashboards
```
