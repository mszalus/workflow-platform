---
title: Admin — Architecture
tags:
  - manual
  - admin
type: manual
source: docs/admin-manual.md
---
[[Manuals MOC]] › [[Admin Portal]] › **Admin — Architecture**

```
┌─────────────┐     ┌─────────────┐
│ Admin Portal│     │ User Portal │
│  :5173      │     │  :5174      │
└──────┬──────┘     └──────┬──────┘
       │ nginx /api proxy  │
       └────────┬──────────┘
                │
         ┌──────▼──────┐
         │   Gateway   │
         │   :9080     │
         └──────┬──────┘
                │
    ┌───────────┼───────────────┬──────────────┐
    ▼           ▼               ▼              ▼
┌────────┐ ┌────────────┐ ┌────────────┐ ┌─────────┐
│Workflow│ │Custom Fields│ │Notification│ │  Audit  │
│ :8081  │ │   :8082    │ │   :8083    │ │  :8084  │
└───┬────┘ └─────┬──────┘ └─────┬──────┘ └────┬────┘
    │            │              │              │
    └────────────┴──────┬───────┴──────────────┘
                        │
              ┌─────────┼──────────┐
              ▼         ▼          ▼
         ┌────────┐ ┌────────┐ ┌────────┐
         │Postgres│ │RabbitMQ│ │Keycloak│
         │ :5433  │ │ :5672  │ │ :8180  │
         └────────┘ └────────┘ └────────┘
```

## Gateway Routing

| Frontend Path          | Gateway Route           | Backend Service        |
|-----------------------|-------------------------|------------------------|
| `/api/workflow/**`    | `RewritePath → /api/**` | [[Workflow Service|workflow-service]]:8081  |
| `/api/fields/**`      | `RewritePath → /api/**` | custom-fields:8082    |
| `/api/notifications/**` | Pass-through          | notification:8083     |
| `/api/audit/**`       | Pass-through            | [[Audit Service|audit-service]]:8084    |

## Event System

Services communicate asynchronously via [[Event System|RabbitMQ]]:

- **Exchange:** `wfp.events` (topic)
- **Routing Keys:** `task.created`, `task.assigned`, `task.completed`, `process.started`, `process.completed`
- **Queues:**
  - `wfp.notification` — binds `task.*` + `process.completed` → creates user notifications
  - `wfp.audit` — binds `#` (all events) → records audit entries

---


---

**Admin manual** — ← [[Admin — Overview]] · [[Admin — Admin Portal]] →

> [!abstract]- All notes in this set
> [[Admin — Overview]]
> [[Admin — Admin Portal]]
> [[Admin — Process Designer]]
> [[Admin — Managing Process Definitions]]
> [[Admin — Custom Field Schemas]]
> [[Admin — Audit Log]]
> [[Admin — Keycloak Administration]]
> [[Admin — RabbitMQ Monitoring]]
> [[Admin — Docker Deployment]]
> [[Admin — Kubernetes Deployment]]
> [[Admin — Troubleshooting]]
