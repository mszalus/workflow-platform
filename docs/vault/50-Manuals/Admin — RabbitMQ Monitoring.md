---
title: Admin — RabbitMQ Monitoring
tags:
  - manual
  - admin
type: manual
source: docs/admin-manual.md
---
[[Manuals MOC]] › [[Admin Portal]] › **Admin — RabbitMQ Monitoring**

**Management UI:** `http://localhost:15672`
**Credentials:** `wfp` / `wfp_secret`

## Key Resources

| Resource | Type     | Description                           |
|----------|----------|---------------------------------------|
| `wfp.events` | Exchange (topic) | All platform events published here |
| `wfp.notification` | Queue | Consumes task.* and [[process.completed]] events |
| `wfp.audit` | Queue | Consumes all events (#)              |

## Monitoring Checklist

1. **Overview** — verify connections from all 4 backend services
2. **Exchanges** — `wfp.events` should have bindings to both queues
3. **Queues** — both queues should show 0 messages (consumed in real-time)
4. **Connections** — 4 connections (one per service)

## Troubleshooting

- **Messages accumulating in queue:** Consumer service may be down. Check container logs.
- **No bindings on exchange:** Services haven't started yet. Wait for Spring Boot initialization.
- **Dead-lettered messages:** Check the service logs for deserialization errors. Ensure `Jackson2JsonMessageConverter` is configured.

---


---

**Admin manual** — ← [[Admin — Keycloak Administration]] · [[Admin — Docker Deployment]] →

> [!abstract]- All notes in this set
> [[Admin — Overview]]
> [[Admin — Architecture]]
> [[Admin — Admin Portal]]
> [[Admin — Process Designer]]
> [[Admin — Managing Process Definitions]]
> [[Admin — Custom Field Schemas]]
> [[Admin — Audit Log]]
> [[Admin — Keycloak Administration]]
> [[Admin — Docker Deployment]]
> [[Admin — Kubernetes Deployment]]
> [[Admin — Troubleshooting]]
