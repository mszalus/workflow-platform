---
title: Flowable Engine
tags:
  - concept
  - flowable
  - bpmn
type: concept
source: services/workflow-service
---

Flowable 7.1.0 runs **embedded, in-process** inside [[Workflow Service]] — it is not a
separate container. It shares the `workflow` PostgreSQL schema, where its `ACT_*` tables
sit alongside the application `wf_*` tables.

## Engine services used

| Flowable API | Wrapped by | Purpose |
|---|---|---|
| `RepositoryService` | `DeploymentService` | deploy BPMN XML, list definitions, fetch XML |
| `RuntimeService` | `ProcessService` | start and cancel instances, variables |
| `IdentityService` | `ProcessService` | set authenticated user so the initiator is recorded |
| `TaskService` | `TaskService` | claim, unclaim, complete, delegate |
| `HistoryService` | `ProcessHistoryService` | completed processes and tasks |

## Native tenant support

Flowable stores a `TENANT_ID_` column on its own tables, so [[Multi-Tenancy|tenant isolation]] for engine
data is handled by passing `tenantId` on every engine call rather than by the Hibernate
filter used for application entities. See [[Multi-Tenancy]].

## Engine events

`FlowableEventListener` subscribes to the engine event bus for `TASK_CREATED`,
`TASK_ASSIGNED` and `PROCESS_COMPLETED` and republishes them to [[Event System|RabbitMQ]]. These
transitions are caused by the engine advancing a process, not by an API call, so they
cannot be published from a controller. See [[Event System]].

> [!warning] H2 test mode
> Flowable integration tests need `MODE=LEGACY` in the H2 JDBC URL. `MODE=PostgreSQL`
> fails on Flowable schema creation. See [[Known Pitfalls]].

## See also

[[Workflow Service]] · [[Admin — Process Designer]] · [[Data Model ERD]]
