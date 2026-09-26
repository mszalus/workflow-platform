---
title: API Endpoint Catalog
tags:
  - reference
  - api
type: reference
source: services/*/controller
---

Every REST endpoint, with the **external** path through the [[API Gateway]] (host `:9080`)
and the internal path on the service. See [[Gateway Routing]] for why two of the four
prefixes are rewritten.

## Workflow Service — `:8081`, prefix `/api/workflow`

| Method | External | Internal | Purpose |
|---|---|---|---|
| `POST` | `/api/workflow/deployments` | `/api/deployments` | Deploy BPMN XML |
| `GET` | `/api/workflow/deployments` | `/api/deployments` | List process definitions |
| `GET` | `/api/workflow/deployments/{processDefinitionId}/bpmn` | … | Export BPMN XML |
| `DELETE` | `/api/workflow/deployments/{deploymentId}` | … | Delete a deployment |
| `POST` | `/api/workflow/processes` | `/api/processes` | Start an instance → [[process.started]] |
| `GET` | `/api/workflow/processes` | `/api/processes` | List active instances (paged) |
| `GET` | `/api/workflow/processes/{id}` | … | Instance detail |
| `DELETE` | `/api/workflow/processes/{id}?reason=` | … | Cancel → [[process.cancelled]] |
| `GET` | `/api/workflow/tasks` | `/api/tasks` | List/filter tasks (paged) |
| `GET` | `/api/workflow/tasks/{id}` | … | Task detail |
| `POST` | `/api/workflow/tasks/{id}/claim` | … | Claim → [[task.assigned]] |
| `POST` | `/api/workflow/tasks/{id}/unclaim` | … | Release back to the group |
| `POST` | `/api/workflow/tasks/{id}/complete` | … | Complete → [[task.completed]] |
| `POST` | `/api/workflow/tasks/{id}/delegate` | … | Delegate → [[task.delegated]] |
| `GET` | `/api/workflow/processes/{processId}/comments` | … | List [[Comment]] rows |
| `POST` | `/api/workflow/processes/{processId}/comments` | … | Add a comment |
| `GET` | `/api/workflow/history/processes` | `/api/history/processes` | Completed instances |
| `GET` | `/api/workflow/history/tasks` | `/api/history/tasks` | Completed tasks |

## Custom Fields Service — `:8082`, prefix `/api/fields`

| Method | External | Internal | Purpose |
|---|---|---|---|
| `POST` | `/api/fields/schemas` | `/api/schemas` | Create a [[FieldSchema]] |
| `GET` | `/api/fields/schemas/{id}` | … | One schema |
| `GET` | `/api/fields/schemas?processDefinitionKey=` | … | Schemas for a definition |
| `DELETE` | `/api/fields/schemas/{id}` | … | Delete a schema (cascades options) |
| `POST` | `/api/fields/values` | `/api/values` | Bulk-save [[FieldValue]] rows |
| `GET` | `/api/fields/values?processInstanceId=` | … | Values for an instance |

## Notification Service — `:8083`, pass-through

| Method | Path (external = internal) | Purpose |
|---|---|---|
| `GET` | `/api/notifications` | Paged list for the caller |
| `GET` | `/api/notifications/unread-count` | Badge count |
| `PUT` | `/api/notifications/mark-read` | Body: `[uuid, …]` |
| `PUT` | `/api/notifications/mark-all-read` | All for the caller |

## Audit Service — `:8084`, pass-through

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/audit` | Filterable, paged [[AuditEntry]] query |

## Conventions

- Every list endpoint returns `PagedResponse<T>` from [[wfp-common]].
- The caller identity comes from `@AuthenticationPrincipal Jwt` — never from a body field.
- The tenant comes from the `X-Tenant-Id` header, never from the payload. See [[Multi-Tenancy]].
- Errors are `ErrorResponse` via `GlobalExceptionHandler`.

## See also

[[Ports and Endpoints]] · [[Security and JWT]] · [[Ports and Endpoints]]
