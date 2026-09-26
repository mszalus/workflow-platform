---
title: Workflow Service
tags:
  - service
  - backend
  - flowable
type: service
source: services/workflow-service
---

> The core of the platform: an embedded [[Flowable Engine]] plus the process and task
> REST API. The **only** service that publishes events.

| | |
|---|---|
| Port | `8081` |
| Module | `services/workflow-service` |
| Schema | `workflow` (app `wf_*` tables + [[Flowable Engine|Flowable]] `ACT_*` tables) |
| Gateway path | `/api/workflow/**` → `/api/**` |

## Endpoints

| Controller | Base path | Operations |
|---|---|---|
| `DeploymentController` | `/api/deployments` | `POST` deploy BPMN · `GET` list definitions · `GET /{id}/bpmn` export XML · `DELETE /{id}` |
| `ProcessController` | `/api/processes` | `POST` start · `GET` list · `GET /{id}` · `DELETE /{id}` cancel |
| `TaskController` | `/api/tasks` | `GET` list · `GET /{id}` · `POST /{id}/claim` · `/unclaim` · `/complete` · `/delegate` |
| `CommentController` | `/api/processes/{processId}/comments` | `GET` list · `POST` add |
| `HistoryController` | `/api/history` | `GET /processes` · `GET /tasks` |

Full signature detail: [[API Endpoint Catalog]].

## Services

| Class | Wraps | Publishes |
|---|---|---|
| `DeploymentService` | Flowable `RepositoryService` | — |
| `ProcessService` | `RuntimeService` + `IdentityService` | [[process.started]], [[process.cancelled]] |
| `TaskService` | Flowable `TaskService` | [[task.completed]], [[task.delegated]] |
| `ProcessHistoryService` | `HistoryService` | — |
| `CommentService` | JPA only (not Flowable comments) | — |
| `EventPublisher` | `RabbitTemplate` | all of the above |
| `FlowableEventListener` | Flowable engine event bus | [[task.created]], [[task.assigned]], [[process.completed]] |

`EventPublisher` injects a **`@Nullable RabbitTemplate`** so test contexts without
RabbitMQ still start — see [[Known Pitfalls]].

## Entities

[[ProcessMetadata]] · [[Comment]] · [[Attachment]]

`ProcessMetadata` carries the single `@FilterDef` for this persistence unit; the other two
declare `@Filter` only. See [[Multi-Tenancy]].

## See also

[[C4 L3 Workflow Service]] · [[Event System]] · [[Event Catalog]] · [[Data Model ERD]]
