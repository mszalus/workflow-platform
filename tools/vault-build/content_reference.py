#!/usr/bin/env python3
"""Reference notes generated from code: events, entities, endpoints, enums, ports."""
import sys, pathlib
sys.path.insert(0, str(pathlib.Path(__file__).parent))
from build_vault import fm, write

R = "35-Reference"

def n(name, tags, source, body, folder=R, ntype="reference", extra=None):
    write(folder, name, fm(name, tags, ntype, source, extra) + body.strip() + "\n")

# ------------------------------------------------------------------ events
EVENTS = [
 ("process.started", "ProcessStartedEvent", "ProcessService", ["Audit Service"],
  [("processInstanceId", "String", "Flowable instance id"),
   ("processDefinitionId", "String", "versioned definition id"),
   ("processDefinitionKey", "String", "stable key across versions"),
   ("processName", "String", "display name"),
   ("businessKey", "String", "caller-supplied correlation key"),
   ("variables", "Map<String, Object>", "initial process variables")],
  "Published explicitly by `ProcessService` when the API starts an instance. Not consumed "
  "by [[Notification Service]] — the queue binds `task.*` and `process.completed` only."),

 ("process.completed", "ProcessCompletedEvent", "FlowableEventListener",
  ["Notification Service", "Audit Service"],
  [("processInstanceId", "String", ""),
   ("processDefinitionKey", "String", ""),
   ("processName", "String", ""),
   ("durationMillis", "long", "wall-clock lifetime of the instance")],
  "Engine-originated: the process ends because the engine reached an end event, not "
  "because anyone called an API. Becomes a `PROCESS_COMPLETED` notification for the initiator."),

 ("process.cancelled", "ProcessCancelledEvent", "ProcessService", ["Audit Service"],
  [("processInstanceId", "String", ""),
   ("processDefinitionKey", "String", ""),
   ("reason", "String", "defaults to `Cancelled by user`")],
  "Published by `DELETE /api/processes/{id}`. Audited but never notified."),

 ("task.created", "TaskCreatedEvent", "FlowableEventListener",
  ["Notification Service", "Audit Service"],
  [("taskId", "String", ""), ("taskName", "String", ""),
   ("taskDefinitionKey", "String", "BPMN element id"),
   ("processInstanceId", "String", ""), ("processDefinitionKey", "String", ""),
   ("assignee", "String", "may be null for unclaimed group tasks"),
   ("dueDate", "Instant", ""), ("priority", "int", "")],
  "Engine-originated. `assignee` is null for tasks offered to a group rather than a person; "
  "`WorkflowEventListener` skips notification creation in that case."),

 ("task.assigned", "TaskAssignedEvent", "FlowableEventListener",
  ["Notification Service", "Audit Service"],
  [("taskId", "String", ""), ("taskName", "String", ""),
   ("processInstanceId", "String", ""),
   ("assignee", "String", "new owner"),
   ("previousAssignee", "String", "null on first assignment")],
  "Fires on claim and on reassignment. Produces a `TASK_ASSIGNED` notification."),

 ("task.completed", "TaskCompletedEvent", "TaskService",
  ["Notification Service", "Audit Service"],
  [("taskId", "String", ""), ("taskName", "String", ""),
   ("processInstanceId", "String", ""), ("processDefinitionKey", "String", ""),
   ("completedBy", "String", ""),
   ("outcome", "Map<String, Object>", "variables submitted with the completion"),
   ("durationMillis", "long", "time the task was open")],
  "Published explicitly by `TaskService.complete`. The `outcome` map carries the form "
  "variables, including values bound to [[Custom Fields Service]] schemas."),

 ("task.delegated", "TaskDelegatedEvent", "TaskService",
  ["Notification Service", "Audit Service"],
  [("taskId", "String", ""), ("taskName", "String", ""),
   ("processInstanceId", "String", ""),
   ("delegatedFrom", "String", ""), ("delegatedTo", "String", ""),
   ("comment", "String", "reason supplied by the delegator")],
  "Published explicitly by `TaskService.delegate`."),
]

for key, cls, producer, consumers, fields, notes in EVENTS:
    rows = "\n".join(f"| `{f}` | `{t}` | {d} |" for f, t, d in fields)
    cons = " · ".join(f"[[{c}]]" for c in consumers)
    n(key, ["event", "reference", f"routing-key/{key.split('.')[0]}"],
      f"libs/wfp-events/src/main/java/com/wfp/events/{cls}.java", f"""
> Routing key `{key}` · class `{cls}`

| | |
|---|---|
| Published by | `{producer}` in [[Workflow Service]] |
| Consumed by | {cons} |
| Exchange | `wfp.events` (topic) |

## Payload

Inherited from `BaseEvent`: `eventId`, `eventType`, `tenantId`, `userId`, `timestamp`.

| Field | Type | Notes |
|---|---|---|
{rows}

## Notes

{notes}

## See also

[[Event Catalog]] · [[Event System]] · [[wfp-events]]
""", extra={"routing_key": key, "java_class": cls})

n("Event Catalog", ["event", "reference", "moc"], "libs/wfp-events", """
Every routing key declared in `EventConstants`, and whether anything actually publishes it.

## Live events

| Routing key | Class | Published by | → Notification | → Audit |
|---|---|---|---|---|
| [[process.started]] | `ProcessStartedEvent` | `ProcessService` | — | ✅ |
| [[process.completed]] | `ProcessCompletedEvent` | `FlowableEventListener` | ✅ | ✅ |
| [[process.cancelled]] | `ProcessCancelledEvent` | `ProcessService` | — | ✅ |
| [[task.created]] | `TaskCreatedEvent` | `FlowableEventListener` | ✅ | ✅ |
| [[task.assigned]] | `TaskAssignedEvent` | `FlowableEventListener` | ✅ | ✅ |
| [[task.completed]] | `TaskCompletedEvent` | `TaskService` | ✅ | ✅ |
| [[task.delegated]] | `TaskDelegatedEvent` | `TaskService` | ✅ | ✅ |

## Declared but not published

| Routing key | Status |
|---|---|
| `process.sla.breached` | constant only — no publisher, no `SlaBreachedEvent` class |
| `field.schema.created` | constant only — [[Custom Fields Service]] publishes nothing |
| `field.value.saved` | constant only — same |

> [!note] The audit queue already covers them
> `wfp.audit` binds `#`, so implementing any of the three requires no binding change on
> [[Audit Service]]. The `wfp.notification` binding (`task.*`, `process.completed`) would
> need widening.

## Infrastructure constants

| Constant | Value |
|---|---|
| `EXCHANGE` | `wfp.events` |
| `NOTIFICATION_QUEUE` | `wfp.notification` |
| `AUDIT_QUEUE` | `wfp.audit` |
| `ALL_EVENTS_ROUTING_KEY` | `#` |

## See also

[[Event System]] · [[wfp-events]] · [[Admin — RabbitMQ Monitoring]]
""")

# ------------------------------------------------------------------ entities
ENTITIES = [
 ("ProcessMetadata", "wf_process_metadata", "workflow", "Workflow Service", True,
  [("id", "UUID", "PK"), ("processDefinitionKey", "String", "not null"),
   ("tenantId", "String", "not null"), ("description", "String", ""),
   ("category", "String", ""), ("iconUrl", "String", ""),
   ("createdAt", "Instant", "not null, immutable"), ("updatedAt", "Instant", "not null")],
  "Presentation metadata for a process definition — the things BPMN XML has no place for. "
  "Keyed by `processDefinitionKey`, so it survives redeployment of a new version.\n\n"
  "Holds the single `@FilterDef` for the `workflow` persistence unit."),

 ("Comment", "wf_comments", "workflow", "Workflow Service", False,
  [("id", "UUID", "PK"), ("processInstanceId", "String", "not null"),
   ("taskId", "String", "nullable — comment may target the instance, not a task"),
   ("userId", "String", "not null"), ("content", "TEXT", "not null"),
   ("createdAt", "Instant", "not null, immutable"), ("tenantId", "String", "not null")],
  "Application-level comments, deliberately **not** Flowable native comments — they are "
  "queryable with plain JPA and survive engine history cleanup."),

 ("Attachment", "wf_attachments", "workflow", "Workflow Service", False,
  [("id", "UUID", "PK"), ("processInstanceId", "String", "not null"),
   ("taskId", "String", "nullable"), ("fileName", "String", "not null"),
   ("contentType", "String", "not null"), ("fileSize", "Long", "not null"),
   ("storageKey", "String", "not null — pointer into blob storage"),
   ("uploadedBy", "String", "not null"), ("createdAt", "Instant", "not null, immutable"),
   ("tenantId", "String", "not null")],
  "> [!warning] No controller yet\n"
  "> The entity and `AttachmentRepository` exist, but no REST endpoint exposes them. "
  "Upload and download are unimplemented; `storageKey` anticipates object storage."),

 ("FieldSchema", "field_schema", "custom_fields", "Custom Fields Service", True,
  [("id", "UUID", "PK"), ("processDefinitionKey", "String", "not null"),
   ("fieldKey", "String", "not null — the variable name"),
   ("label", "String", "not null"), ("fieldType", "FieldType", "enum, not null"),
   ("required", "boolean", "not null"), ("sortOrder", "int", "render order"),
   ("defaultValue", "String", ""), ("placeholder", "String", ""),
   ("validationRegex", "String", "client and server validation"),
   ("tenantId", "String", "not null"), ("createdAt", "Instant", ""),
   ("updatedAt", "Instant", ""), ("options", "List<FieldOption>", "cascade ALL, orphan removal")],
  "One user-defined field on one process definition. Holds the `@FilterDef` for the "
  "`custom_fields` persistence unit. Types are listed in [[Enumerations]]."),

 ("FieldOption", "field_option", "custom_fields", "Custom Fields Service", None,
  [("id", "UUID", "PK"), ("fieldSchema", "FieldSchema", "`@ManyToOne(LAZY)`, not null"),
   ("label", "String", "not null"), ("value", "String", "not null"),
   ("sortOrder", "int", "")],
  "Choices for a `DROPDOWN` or `MULTI_SELECT` [[FieldSchema]].\n\n"
  "> [!info] No `tenant_id` column\n"
  "> The only entity without one. It is reachable exclusively through its parent schema, "
  "which is already tenant-filtered — see [[Multi-Tenancy]]."),

 ("FieldValue", "field_value", "custom_fields", "Custom Fields Service", False,
  [("id", "UUID", "PK"), ("fieldSchemaId", "UUID", "not null — **no FK constraint**"),
   ("processInstanceId", "String", "not null — points into the `workflow` schema"),
   ("value", "TEXT", "every type serialized as text"),
   ("tenantId", "String", "not null"), ("createdAt", "Instant", ""), ("updatedAt", "Instant", "")],
  "The captured answer for one field on one process instance. Note that it references "
  "`FieldSchema` by raw UUID rather than a JPA association, and `processInstanceId` crosses "
  "a schema boundary with no referential integrity. See [[Data Model ERD]]."),

 ("Notification", "notification", "notification", "Notification Service", True,
  [("id", "UUID", "PK"), ("userId", "String", "not null — recipient"),
   ("tenantId", "String", "not null"), ("title", "String", "not null"),
   ("message", "TEXT", ""), ("type", "NotificationType", "enum, not null"),
   ("read", "boolean", "not null, default false"),
   ("referenceId", "String", "task or process instance id"),
   ("referenceType", "String", "which of the two"),
   ("createdAt", "Instant", "immutable")],
  "Created only by `WorkflowEventListener` from a consumed event — never by an API call. "
  "Holds the `@FilterDef` for the `notification` persistence unit."),

 ("NotificationPreference", "notification_preference", "notification", "Notification Service", False,
  [("id", "UUID", "PK"), ("userId", "String", "not null"),
   ("tenantId", "String", "not null"), ("eventType", "String", "not null"),
   ("emailEnabled", "boolean", "default true"), ("inAppEnabled", "boolean", "default true")],
  "> [!warning] Defined but not enforced\n"
  "> Nothing consults this table before creating a [[Notification]]. Wiring it into "
  "`WorkflowEventListener` is a planned enhancement; email delivery does not exist at all."),

 ("AuditEntry", "audit_entry", "audit", "Audit Service", True,
  [("id", "UUID", "PK"), ("eventType", "String", "not null — the routing key"),
   ("entityType", "String", "not null — e.g. `task`, `process`"),
   ("entityId", "String", "not null"), ("userId", "String", "who caused it"),
   ("tenantId", "String", "not null"), ("timestamp", "Instant", "not null"),
   ("details", "TEXT", "serialized event payload"),
   ("sourceService", "String", "which service emitted it")],
  "One row per event on the bus — the `wfp.audit` queue binds `#`, so this table is the "
  "complete history of everything the platform has published. Queried through "
  "`AuditEntrySpecification`."),
]

for name, table, schema, service, filterdef, fields, notes in ENTITIES:
    rows = "\n".join(f"| `{f}` | `{t}` | {d} |" for f, t, d in fields)
    if filterdef is True:
        tenancy = "Declares the single **`@FilterDef`** for this persistence unit, plus `@Filter`."
    elif filterdef is False:
        tenancy = "Declares **`@Filter` only** — the `@FilterDef` lives on another entity in this service."
    else:
        tenancy = "**No tenant column.** Filtered indirectly through its parent."
    n(name, ["entity", "reference", f"schema/{schema}"],
      f"services/{service.lower().replace(' ', '-')}", f"""
> Table `{table}` · schema `{schema}` · owned by [[{service}]]

## Columns

| Field | Type | Notes |
|---|---|---|
{rows}

## Tenancy

{tenancy} See [[Multi-Tenancy]].

## Notes

{notes}

## See also

[[Data Model ERD]] · [[Entity Reference]] · [[{service}]]
""", extra={"table": table, "schema": schema})

n("Entity Reference", ["entity", "reference", "moc"], "services/*/entity", """
All nine JPA entities across four schemas. Flowable `ACT_*` tables are engine-managed and
not mapped — see [[Flowable Engine]].

| Entity | Table | Schema | Service | Tenant column |
|---|---|---|---|---|
| [[ProcessMetadata]] | `wf_process_metadata` | `workflow` | [[Workflow Service]] | ✅ `@FilterDef` |
| [[Comment]] | `wf_comments` | `workflow` | [[Workflow Service]] | ✅ |
| [[Attachment]] | `wf_attachments` | `workflow` | [[Workflow Service]] | ✅ |
| [[FieldSchema]] | `field_schema` | `custom_fields` | [[Custom Fields Service]] | ✅ `@FilterDef` |
| [[FieldOption]] | `field_option` | `custom_fields` | [[Custom Fields Service]] | ❌ via parent |
| [[FieldValue]] | `field_value` | `custom_fields` | [[Custom Fields Service]] | ✅ |
| [[Notification]] | `notification` | `notification` | [[Notification Service]] | ✅ `@FilterDef` |
| [[NotificationPreference]] | `notification_preference` | `notification` | [[Notification Service]] | ✅ |
| [[AuditEntry]] | `audit_entry` | `audit` | [[Audit Service]] | ✅ `@FilterDef` |

## Cross-schema references (no FKs)

```mermaid
flowchart LR
    subgraph workflow
      ACT["ACT_RU_EXECUTION<br/>(Flowable)"]
      WFC["wf_comments"]
      WFA["wf_attachments"]
      WFM["wf_process_metadata"]
    end
    subgraph custom_fields
      FV["field_value"]
      FS["field_schema"]
    end
    subgraph notification
      NT["notification"]
    end
    subgraph audit
      AE["audit_entry"]
    end
    WFC -.->|processInstanceId| ACT
    WFA -.->|processInstanceId| ACT
    FV  -.->|processInstanceId| ACT
    FV  -->|fieldSchemaId| FS
    NT  -.->|referenceId| ACT
    AE  -.->|entityId| ACT
```

Dotted edges cross a schema boundary and carry **no referential integrity** — they are
string ids. Deleting a process instance orphans rows in three other schemas.

## See also

[[Data Model ERD]] · [[Enumerations]] · [[Multi-Tenancy]]
""")

n("Enumerations", ["reference", "enum"], "services/*/entity/*Type.java", """
## FieldType

Drives which control `DynamicFieldForm.tsx` renders for a [[FieldSchema]].

| Value | Rendered as |
|---|---|
| `TEXT` | single-line input |
| `TEXTAREA` | multi-line input |
| `NUMBER` | numeric input |
| `DATE` | date picker |
| `DATETIME` | date + time picker |
| `BOOLEAN` | checkbox |
| `DROPDOWN` | single select, needs [[FieldOption]] rows |
| `MULTI_SELECT` | multi select, needs [[FieldOption]] rows |
| `FILE` | file picker — depends on [[Attachment]], which has no endpoint yet |
| `USER_PICKER` | user lookup |

All values are stored as `TEXT` in [[FieldValue]] regardless of type.

## NotificationType

| Value | Produced by |
|---|---|
| `TASK_ASSIGNED` | [[task.created]], [[task.assigned]] |
| `TASK_COMPLETED` | [[task.completed]] |
| `PROCESS_COMPLETED` | [[process.completed]] |
| `SLA_BREACH` | nothing yet — `process.sla.breached` has no publisher |
| `INFO` | reserved for manual or system notices |

## See also

[[Entity Reference]] · [[Event Catalog]] · [[Custom Fields Service]]
""")

n("API Endpoint Catalog", ["reference", "api"], "services/*/controller", """
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

[[Ports and Endpoints]] · [[Security and JWT]] · [[README — API Endpoints|README summary]]
""")

n("Ports and Endpoints", ["reference", "ops", "ports"], "docker/docker-compose.yml", """
Host ports as mapped by Docker Compose. Note PostgreSQL is deliberately **5433** on the
host to avoid colliding with a local install — see [[Known Pitfalls]].

## Application

| Service | Container | Host | URL |
|---|---|---|---|
| [[Admin Portal]] | 8080 | **5173** | http://localhost:5173 |
| [[User Portal]] | 8080 | **5174** | http://localhost:5174 |
| [[API Gateway]] | 8080 | **9080** | http://localhost:9080 |
| [[Workflow Service]] | 8081 | 8081 | http://localhost:8081/actuator/health |
| [[Custom Fields Service]] | 8082 | 8082 | http://localhost:8082/actuator/health |
| [[Notification Service]] | 8083 | 8083 | http://localhost:8083/actuator/health |
| [[Audit Service]] | 8084 | 8084 | http://localhost:8084/actuator/health |

## Infrastructure

| Component | Host port | URL / note |
|---|---|---|
| PostgreSQL 16 | **5433** | 5432 inside the network |
| RabbitMQ | 5672 | AMQP |
| RabbitMQ management | 15672 | http://localhost:15672 |
| Keycloak 25 | **8180** | http://localhost:8180 |

## Observability

| Component | Host port | URL / note |
|---|---|---|
| OTel Collector | 4317 / 4318 | gRPC / HTTP OTLP ingest |
| OTel Collector metrics | 8888 | scraped by Prometheus |
| Tempo | 3200 | trace query API |
| Prometheus | 9090 | http://localhost:9090 |
| Grafana | 3000 | http://localhost:3000 |

## Schemas in the shared PostgreSQL instance

`workflow` · `custom_fields` · `notification` · `audit` · `keycloak`

Created by `docker/init-db.sql`.

## See also

[[Docker Compose Stack]] · [[Observability Stack]] · [[Quick Start]]
""")

print("reference written")
