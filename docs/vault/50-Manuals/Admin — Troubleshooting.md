---
title: Admin — Troubleshooting
tags:
  - manual
  - admin
type: manual
source: docs/admin-manual.md
---
[[Manuals MOC]] › [[Admin Portal]] › **Admin — Troubleshooting**

## Common Issues

| Symptom | Cause | Fix |
|---------|-------|-----|
| Service fails to start with `DataSource` error | Missing database schema | Run `docker/init-db.sql` against PostgreSQL |
| `Failed to configure a DataSource` on gateway | Gateway doesn't need a DB | Verify gateway excludes `DataSourceAutoConfiguration` |
| Duplicate `@FilterDef` error | Two entities define `@FilterDef(name = "tenantFilter")` | Only ONE entity per persistence unit should have `@FilterDef`; others use `@Filter` only |
| [[Event System|RabbitMQ]] connection refused | RabbitMQ not ready | Wait for RabbitMQ health check to pass |
| JWT validation fails | [[Security and JWT|Keycloak]] not reachable | Check Keycloak is running and `issuer-uri` is correct |
| Frontend shows "Loading..." | API calls failing | Check browser console for errors; verify gateway is running |
| Port 5432 conflict | Local PostgreSQL running | Docker maps PG to port 5433 to avoid conflicts |
| `gradlew` permission denied | File not executable | Run `git update-index --chmod=+x gradlew` |
| [[Flowable Engine|Flowable]] + H2 test failures | Wrong H2 mode | Use `MODE=LEGACY` in JDBC URL, not `MODE=PostgreSQL` |

## Health Checks

Verify all services are healthy:

```bash
# Gateway
curl http://localhost:9080/actuator/health

# Individual services
curl http://localhost:8081/actuator/health  # workflow
curl http://localhost:8082/actuator/health  # custom-fields
curl http://localhost:8083/actuator/health  # notification
curl http://localhost:8084/actuator/health  # audit
```

## Useful Commands

```bash
# Get a JWT token
curl -s -X POST 'http://localhost:8180/realms/workflow-platform/protocol/openid-connect/token' \
  -d 'grant_type=password&client_id=wfp-admin-portal&username=admin-a&password=password' \
  | jq .access_token -r

# Deploy a BPMN process
curl -X POST http://localhost:9080/api/workflow/deployments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"My Process","bpmnXml":"<xml>..."}'

# List tasks
curl http://localhost:9080/api/workflow/tasks?assignee=admin-a \
  -H "Authorization: Bearer $TOKEN"

# View audit log
curl http://localhost:9080/api/audit \
  -H "Authorization: Bearer $TOKEN"
```


---

**Admin manual** — ← [[Admin — Kubernetes Deployment]]

> [!abstract]- All notes in this set
> [[Admin — Overview]]
> [[Admin — Architecture]]
> [[Admin — Admin Portal]]
> [[Admin — Process Designer]]
> [[Admin — Managing Process Definitions]]
> [[Admin — Custom Field Schemas]]
> [[Admin — Audit Log]]
> [[Admin — Keycloak Administration]]
> [[Admin — RabbitMQ Monitoring]]
> [[Admin — Docker Deployment]]
> [[Admin — Kubernetes Deployment]]
