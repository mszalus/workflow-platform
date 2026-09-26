#!/usr/bin/env python3
"""Service and library notes, generated from the source tree."""
import sys, pathlib
sys.path.insert(0, str(pathlib.Path(__file__).parent))
from build_vault import fm, write

def n(folder, name, tags, ntype, source, body, extra=None):
    write(folder, name, fm(name, tags, ntype, source, extra) + body.strip() + "\n")

SVC = "20-Services"

n(SVC, "API Gateway", ["service", "backend", "gateway"], "service",
  "services/gateway", """
> Thin routing + security edge. **No database.**

| | |
|---|---|
| Port | `8080` (container) / `9080` (host) |
| Module | `services/gateway` |
| Framework | Spring Cloud Gateway **MVC** (servlet, not reactive) |
| Schema | — none — |

## Responsibilities

1. Validate the JWT signature against the Keycloak JWK Set.
2. Extract `tenant_id` from the token and forward it as `X-Tenant-Id` — see [[Multi-Tenancy]].
3. Rewrite and route `/api/**` paths to the four backend services — see [[Gateway Routing]].
4. Expose a public `/actuator/health` for probes.

## Classes

| Class | Role |
|---|---|
| `GatewayApplication` | Boot entrypoint; excludes `DataSourceAutoConfiguration` and `HibernateJpaAutoConfiguration` |
| `GatewaySecurityConfig` | OAuth2 resource server, public endpoint whitelist |
| `CorsConfig` | Allowed origins for the two portals |
| `TenantHeaderFilter` | Reads the `tenant_id` claim, sets `X-Tenant-Id` downstream |

## Why it excludes JPA autoconfiguration

The gateway depends on [[wfp-security]], which pulls Spring Data JPA transitively. Without
the exclusion, Boot fails at startup looking for a `DataSource` the gateway does not have.
Recorded in [[Known Pitfalls]].

## See also

[[C4 L3 API Gateway]] · [[Gateway Routing]] · [[API Endpoint Catalog]] · [[Ports and Endpoints]]
""")

n(SVC, "Workflow Service", ["service", "backend", "flowable"], "service",
  "services/workflow-service", """
> The core of the platform: an embedded [[Flowable Engine]] plus the process and task
> REST API. The **only** service that publishes events.

| | |
|---|---|
| Port | `8081` |
| Module | `services/workflow-service` |
| Schema | `workflow` (app `wf_*` tables + Flowable `ACT_*` tables) |
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
""")

n(SVC, "Custom Fields Service", ["service", "backend"], "service",
  "services/custom-fields-service", """
> Attaches arbitrary user-defined fields to any process definition, and stores their
> values per process instance. Neither publishes nor consumes events today.

| | |
|---|---|
| Port | `8082` |
| Module | `services/custom-fields-service` |
| Schema | `custom_fields` |
| Gateway path | `/api/fields/**` → `/api/**` |

## Endpoints

| Controller | Base path | Operations |
|---|---|---|
| `FieldSchemaController` | `/api/schemas` | `POST` create · `GET /{id}` · `GET ?processDefinitionKey=` · `DELETE /{id}` |
| `FieldValueController` | `/api/values` | `POST` save (bulk) · `GET ?processInstanceId=` |

## Model

A [[FieldSchema]] defines one field on one `processDefinitionKey`; a `DROPDOWN` or
`MULTI_SELECT` schema owns a list of [[FieldOption]]; a [[FieldValue]] is the answer
captured for one process instance.

```
FieldSchema 1 ──< FieldOption
FieldSchema 1 ──< FieldValue   (by fieldSchemaId, no FK constraint)
```

Field types are listed in [[Enumerations]].

## Cross-service coupling

`FieldValue.processInstanceId` and `FieldSchema.processDefinitionKey` point at Flowable
rows in the `workflow` schema with **no foreign key** — the schemas are independent.
See [[Data Model ERD]].

The end-user form is rendered by `DynamicFieldForm.tsx` in the [[User Portal]]; the
schemas are authored in `CustomFieldEditor.tsx` in the [[Admin Portal]].

## See also

[[Admin — Custom Field Schemas]] · [[API Endpoint Catalog]]
""")

n(SVC, "Notification Service", ["service", "backend", "events"], "service",
  "services/notification-service", """
> Entirely event-driven. Nothing creates a notification synchronously — every row
> arrives over RabbitMQ.

| | |
|---|---|
| Port | `8083` |
| Module | `services/notification-service` |
| Schema | `notification` |
| Gateway path | `/api/notifications/**` (pass-through, no rewrite) |
| Queue | `wfp.notification`, binds `task.*` and `process.completed` |

## Endpoints

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/notifications` | Paginated list for the caller |
| `GET` | `/api/notifications/unread-count` | `{ "count": n }` |
| `PUT` | `/api/notifications/mark-read` | Body: array of notification UUIDs |
| `PUT` | `/api/notifications/mark-all-read` | All for the caller |

## Event to notification mapping

| Incoming event | NotificationType | Target user |
|---|---|---|
| [[task.created]] | `TASK_ASSIGNED` | task assignee |
| [[task.assigned]] | `TASK_ASSIGNED` | task assignee |
| [[task.completed]] | `TASK_COMPLETED` | completer |
| [[process.completed]] | `PROCESS_COMPLETED` | process initiator |

`WorkflowEventListener` tolerates a null `userId` (unassigned tasks) by skipping the row.

## Entities

[[Notification]] · [[NotificationPreference]]

> [!warning] Not yet wired
> `NotificationPreference` exists and is queryable but the listener does **not** consult it
> before creating a notification. Email delivery is likewise unimplemented.

## See also

[[C4 L3 Notification Service]] · [[Event System]] · [[User — Notifications]] · [[Enumerations]]
""")

n(SVC, "Audit Service", ["service", "backend", "events"], "service",
  "services/audit-service", """
> Write-only sink for every event on the bus, plus a query API over the trail.

| | |
|---|---|
| Port | `8084` |
| Module | `services/audit-service` |
| Schema | `audit` |
| Gateway path | `/api/audit/**` (pass-through, no rewrite) |
| Queue | `wfp.audit`, binds `#` — **all** routing keys |

## Endpoints

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/audit` | Paginated, filterable query over [[AuditEntry]] |

Filtering is built with `AuditEntrySpecification` (JPA Criteria) over entity type,
entity id, user, event type and time range.

## Why it binds `#`

Audit is deliberately the catch-all consumer: any new routing key added to the
[[Event System]] is captured without touching this service. Contrast with
[[Notification Service]], whose bindings must be widened explicitly.

## Entities

[[AuditEntry]] — carries `sourceService`, so the trail records which service emitted the row.

## See also

[[Admin — Audit Log]] · [[Event Catalog]]
""")

n(SVC, "Admin Portal", ["frontend", "react"], "service",
  "frontend/apps/admin-portal", """
> React 18 + TypeScript + Vite SPA for process designers and platform admins.
> Served by nginx in Docker; host port `5173`.

## Pages

| Page | Purpose |
|---|---|
| `Dashboard.tsx` | Deployment counts, recent activity |
| `ProcessDesigner.tsx` | bpmn-js canvas — draw, import, export, deploy |
| `ProcessList.tsx` | Deployed definitions, delete |
| `CustomFieldEditor.tsx` | Author [[FieldSchema]] rows per process definition |
| `AuditLog.tsx` | Query the [[Audit Service]] trail |

Shared pieces come from [[shared-ui]]; the modeller from [[bpmn-editor]].

## See also

[[Frontend Architecture]] · [[Admin — Admin Portal]] · [[Admin — Process Designer]] · [[Screenshot Gallery]]
""")

n(SVC, "User Portal", ["frontend", "react"], "service",
  "frontend/apps/user-portal", """
> React 18 + TypeScript + Vite SPA for day-to-day workflow participants.
> Served by nginx in Docker; host port `5174`.

## Pages

| Page | Purpose |
|---|---|
| `Dashboard.tsx` | Task counts, unread notifications |
| `TaskInbox.tsx` | My tasks + tasks available to claim |
| `TaskDetail.tsx` | Task form, comments, complete and delegate |
| `StartProcess.tsx` | Pick a definition and start it |
| `MyProcesses.tsx` | Instances the user started |
| `Notifications.tsx` | List, mark read |

`DynamicFieldForm.tsx` renders the [[Custom Fields Service]] schema for a process at
start and at task completion.

## See also

[[Frontend Architecture]] · [[User — Task Inbox]] · [[User — Completing a Task]] · [[Screenshot Gallery]]
""")

LIBS = [
 ("wfp-common", "libs/wfp-common", "backend", """
Cross-cutting response shapes and error handling. Consumed by all four backend services.

| Type | Purpose |
|---|---|
| `ErrorResponse` | Uniform error body (timestamp, status, message, path) |
| `PagedResponse<T>` | Page envelope used by every list endpoint |
| `GlobalExceptionHandler` | `@RestControllerAdvice` mapping exceptions to `ErrorResponse` |

Built with the `wfp.library-conventions` Gradle plugin — see [[Build System]].
"""),
 ("wfp-events", "libs/wfp-events", "backend", """
The event contract shared by publisher and consumers. Changing a class here is a
**wire-format change** affecting [[Workflow Service]], [[Notification Service]] and
[[Audit Service]] simultaneously.

- `BaseEvent` — abstract root: `eventId`, `eventType`, `tenantId`, `userId`, `timestamp`.
  Uses `@JsonTypeInfo(use = NAME, property = "eventType")` so the concrete type is
  recoverable on the consumer side.
- `EventConstants` — exchange name, every routing key, both queue names.
- Seven concrete event classes — see [[Event Catalog]].

Deserialization requires a `Jackson2JsonMessageConverter` bean in each consumer
`RabbitMQConfig`; without it Spring AMQP hands the listener a `byte[]`.
See [[Known Pitfalls]].
"""),
 ("wfp-security", "libs/wfp-security", "backend", """
The multi-tenancy and authentication machinery, shared by every backend service — and,
awkwardly, by the [[API Gateway]], which is why the gateway must exclude JPA
autoconfiguration.

| Class | Role |
|---|---|
| `SecurityConfig` | OAuth2 resource server, JWT decoder, public endpoint whitelist |
| `JwtTenantConverter` | Maps Keycloak realm roles to authorities, reads `tenant_id` |
| `TenantContext` | `ThreadLocal<String>` holding the current tenant |
| `TenantInterceptor` | Reads `X-Tenant-Id` on each request, populates `TenantContext` |
| `TenantFilterAspect` | AOP: enables the Hibernate `tenantFilter` per request |
| `TenantHibernateFilter` | Filter definition plumbing |

See [[Multi-Tenancy]] and [[Security and JWT]].
"""),
 ("wfp-test-support", "libs/wfp-test-support", "backend", """
Test-only fixtures shared across service test suites.

| Class | Role |
|---|---|
| `JwtTestHelper` | Mints mock JWTs with tenant and role claims for MockMvc tests |
| `TenantTestHelper` | Sets and clears `TenantContext` around service-layer tests |
| `TestContainersConfig` | Shared PostgreSQL + RabbitMQ Testcontainers definitions |

See [[Testing Strategy]].
"""),
 ("bpmn-editor", "frontend/packages/bpmn-editor", "frontend", """
A thin React wrapper around **bpmn-js**, plus Flowable-specific property panel support.

| File | Role |
|---|---|
| `BpmnEditor.tsx` | The canvas component (import/export XML, palette, modelling) |
| `FlowablePropertiesProvider.ts` | Adds Flowable extension properties to the panel |
| `flowable.json` | The Flowable moddle descriptor |

Consumed only by the [[Admin Portal]] process designer. Build order matters: this package
builds after [[shared-ui]] — see [[Known Pitfalls]].
"""),
 ("shared-ui", "frontend/packages/shared-ui", "frontend", """
Shared React building blocks for both portals.

| Area | Contents |
|---|---|
| `api/apiClient.ts` | fetch wrapper: base URL, bearer token injection, error mapping |
| `auth/AuthProvider.tsx` | OIDC context — login redirect, token refresh, claims |
| `types/` | Shared DTO types mirroring the backend contracts |

Must be built **first** in the npm workspace — see [[Frontend Architecture]].
"""),
]
for name, src, side, body in LIBS:
    n(SVC, name, ["library", side], "library", src, body)

print("services + libs written")
