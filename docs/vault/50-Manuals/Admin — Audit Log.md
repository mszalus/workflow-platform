---
title: Admin — Audit Log
tags:
  - manual
  - admin
type: manual
source: docs/admin-manual.md
---
[[Manuals MOC]] › [[Admin Portal]] › **Admin — Audit Log**

Navigate to **Audit Log** in the sidebar.

The audit log captures every significant event in the platform, recorded asynchronously via [[Event System|RabbitMQ]].

## Viewing the Audit Log

The table displays audit entries with columns:

| Column    | Description                          |
|-----------|--------------------------------------|
| Timestamp | When the event occurred              |
| Event     | Event type (e.g., `task.completed`)  |
| Entity    | Entity type (PROCESS or TASK)        |
| Entity ID | ID of the affected entity            |
| User      | User who triggered the event         |

## Filtering

- **Type filter** — filter by entity type (All, Process, Task)
- **User ID** — filter by user who performed the action
- **Pagination** — navigate through pages (20 entries per page)

## Event Types

| Event Type         | Description                        |
|-------------------|------------------------------------|
| `process.started` | A new process instance was created |
| `process.completed`| A process instance finished       |
| `task.created`    | A new task was created             |
| `task.assigned`   | A task was assigned to a user      |
| `task.completed`  | A task was marked as complete      |

---


---

**Admin manual** — ← [[Admin — Custom Field Schemas]] · [[Admin — Keycloak Administration]] →

> [!abstract]- All notes in this set
> [[Admin — Overview]]
> [[Admin — Architecture]]
> [[Admin — Admin Portal]]
> [[Admin — Process Designer]]
> [[Admin — Managing Process Definitions]]
> [[Admin — Custom Field Schemas]]
> [[Admin — Keycloak Administration]]
> [[Admin — RabbitMQ Monitoring]]
> [[Admin — Docker Deployment]]
> [[Admin — Kubernetes Deployment]]
> [[Admin — Troubleshooting]]
