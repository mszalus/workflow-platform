---
title: Step 15 — Local Observability Verification
tags:
  - project
  - plan
  - status/done
type: plan
source: PLAN.md
status: done
---
[[Project MOC]] › **Step 15 — Local Observability Verification**

**Goal:** Verify that traces appear in Tempo, metrics appear in [[Observability Stack|Prometheus]], and structured logs contain the right fields — all running locally via docker-compose.

## Prerequisites

- Full stack running: `docker compose -f docker/docker-compose.yml up -d`
- Wait for all services to be healthy (gateway health: `curl http://localhost:9080/actuator/health`)
- New containers added: `wfp-otel-collector`, `wfp-tempo`, `wfp-prometheus`, `wfp-grafana`
- Note: backend service images must be rebuilt after the observability commit to pick up new JARs:
  ```bash
  docker compose -f docker/docker-compose.yml build \
    gateway workflow-service custom-fields-service notification-service audit-service
  docker compose -f docker/docker-compose.yml up -d
  ```

## 15.1 Verify Prometheus scraping

1. Open `http://localhost:9090/targets` — all 5 services + `otel-collector` must show **State: UP**
2. If any show DOWN, check `docker logs wfp-prometheus` and verify the service container is running
3. Spot-check a metric in the Prometheus query UI:
   ```promql
   http_server_requests_seconds_count{application="workflow-service"}
   ```
4. Generate traffic first if needed:
   ```bash
   TOKEN=$(curl -s -X POST http://localhost:8180/realms/workflow-platform/protocol/openid-connect/token \
     -d "grant_type=password&client_id=wfp-admin-portal&username=admin-a&password=password" \
     | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")
   curl -s -H "Authorization: Bearer $TOKEN" http://localhost:9080/api/workflow/deployments | python3 -m json.tool
   ```
5. Query JVM metrics: `jvm_memory_used_bytes{application="workflow-service"}`

## 15.2 Verify distributed tracing in Grafana + Tempo

1. Open `http://localhost:3000` (anonymous access, no login required)
2. Go to **Explore** → select **Tempo** datasource
3. Set **Query type: Search**, click **Run query** — traces from all services should appear
4. Click any trace to see the span waterfall: gateway → [[Workflow Service|workflow-service]] (or whichever service handled the request)
5. Verify span attributes include `tenantId` (set via MDC) and `http.route`
6. In the **Grafana Explore** panel, switch to **Prometheus** datasource and verify `up` metric shows all targets

## 15.3 Verify structured logs

1. Inspect a backend service container's stdout:
   ```bash
   docker logs wfp-workflow --tail 20
   ```
   Each line should be a single JSON object with fields: `time`, `severity`, `message`, `logger`, `thread`, `service`, `traceId`, `spanId`, `tenantId`, `userId`

2. Verify `severity` uses GCP values (INFO, WARNING, ERROR — not WARN):
   ```bash
   docker logs wfp-workflow 2>&1 | python3 -c "
   import sys, json
   for line in sys.stdin:
       try:
           obj = json.loads(line)
           print(obj.get('severity'), '|', obj.get('tenantId'), '|', obj.get('message','')[:60])
       except: pass
   " | head -20
   ```

3. Trigger a WARN-level log by making an unauthenticated request and verify `severity: WARNING` appears (not WARN):
   ```bash
   curl -s http://localhost:9080/api/workflow/deployments  # no token → 401
   docker logs wfp-gateway --tail 5
   ```

4. Verify `tenantId` appears on authenticated requests:
   ```bash
   curl -s -H "Authorization: Bearer $TOKEN" http://localhost:9080/api/workflow/deployments > /dev/null
   docker logs wfp-workflow --tail 5 | python3 -c "import sys,json; [print(json.loads(l).get('tenantId','(none)')) for l in sys.stdin if l.strip()]"
   ```

## 15.4 Verify trace–log correlation (manual)

1. Make an API call and note the `traceId` from the response log:
   ```bash
   curl -s -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"processDefinitionKey":"test"}' \
     http://localhost:9080/api/workflow/processes
   docker logs wfp-workflow --tail 3 | python3 -c "import sys,json; [print(json.loads(l).get('traceId')) for l in sys.stdin if l.strip()]"
   ```
2. Take the `traceId`, open Grafana Explore → Tempo → **TraceQL** → `{ .traceId = "<id>" }` — the full trace should appear

## 15.5 Run BDD acceptance tests to confirm nothing regressed

```bash
JAVA_HOME='C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.4\jbr' \
  ./gradlew :tests:bdd-acceptance:test --no-daemon
```
All 20 scenarios must pass.

**Verification checklist:**
- [ ] Prometheus shows all 6 scrape targets as UP
- [ ] Grafana Tempo shows traces with multi-span waterfalls
- [ ] Container logs are JSON with `severity`, `traceId`, `tenantId`, `userId`
- [ ] `severity` uses GCP values (WARNING not WARN)
- [ ] All 20 BDD scenarios pass

---


---

**Plan** — ← [[Step 14 — Observability and BDD Acceptance Tests]] · [[Step 16 — GCP Infrastructure Terraform Helm Preparation]] →

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
> [[Step 14 — Observability and BDD Acceptance Tests]]
> [[Step 16 — GCP Infrastructure Terraform Helm Preparation]]
> [[Step 17 — GCP Deployment and Acceptance Testing]]
> [[GCP Cost Estimate]]
> [[Step 18 — Release and Rollback Strategy]]
> [[Step 19 — GCP Observability Readiness no deployment]]
> [[Commit Strategy]]
