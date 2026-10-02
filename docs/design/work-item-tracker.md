# Work Item Tracker on BPMN

Design agreed 2026-09-26, moved from `PLAN.md` step 20. Delivery is tracked in the GitHub milestone "Phase 1: Work item tracker". Screens: [tracker-screens.md](tracker-screens.md). Target architecture and delivery order: [tracker-architecture.md](tracker-architecture.md).

**Goal:** Turn the platform into a simple issue tracker. A **work item** has a few fixed fields and any number of custom fields. Its lifecycle is a BPMN process drawn in the existing editor. The BPMN engine sits behind a small facade, so Flowable can later be swapped for Camunda 7, Operaton or Activiti.

**Target use case:** a service desk / request tracker rather than a Jira Software clone. The user portal already works as a request portal, and BPMN automation (approvals, SLAs) is what sets this apart from Jira-lite tools.

## 20.1 Decisions

| Topic | Decision | Why |
|---|---|---|
| Record of truth | New `wf_item` table. Every item owns exactly one process run | Items must be listable, searchable and editable without going through the engine |
| Where it lives | In `workflow-service`, in the same schema and transaction as Flowable | The status copy and the engine state commit together, so no cross-service sync or events are needed to stay consistent |
| One model | Every process run belongs to an item. "Start process" becomes "create item"; the task inbox becomes "my items" | Avoids two parallel concepts (bare processes and items) |
| Fixed fields | `key` (`PROJ-123`, per-project sequence), `project`, `type`, `title`, `description`, `status`, `priority`, `assignee`, `reporter`, `created_at`, `updated_at` | Everything else is a custom field |
| Custom field values | `jsonb` column on `wf_item`: GIN index for filters, expression indexes for sorted fields | Lists filter and sort on fixed and custom fields in one SQL query |
| Custom field definitions | Move `FieldSchema`/`FieldOption` into `workflow-service` and scope them by project + item type. Retire `custom-fields-service` | Values and definitions sit next to the items; one service fewer |
| Status | A BPMN **user task** (a wait state) marked with `wfp:statusCategory` = `OPEN` \| `TODO` \| `IN_PROGRESS` \| `DONE` (`OPEN` added 2026-10-01, screen decision S-2) | The category drives board columns and "resolved" semantics |
| Transitions | The **named outgoing flows** of the exclusive gateway directly after a status. The user picks one. Deploy adds the condition `${transition == '<flowId>'}` to each of those flows that has none (changed from "the editor generates it" in 20.c, 2026-10-02: the condition syntax is an engine detail, and imported BPMN works too). A status with a single outgoing flow has one transition | Multiple paths = multiple buttons. Gateways after service tasks remain automatic routing |
| Transitions from any status | An interrupting message event subprocess (e.g. *Cancel*) | Avoids drawing an arrow from every status |
| Reopen | *Done* is a user task with a *Reopen* flow. The process ends only at an end event (*Closed*) | A finished process cannot be resumed |
| Parallel work | Only inside a subprocess that counts as one status (e.g. *In Approval* with two parallel approvals) | An item is always in exactly one status |
| Engine facade | A `WorkflowEngine` interface in tracker terms, with a single Flowable adapter. Stored BPMN keeps the `flowable:*` attributes | Swapping to Camunda 7 or Operaton means a new adapter plus a one-time `flowable:` → `camunda:` rewrite of stored XML |

**Deferred (judged over-engineering for now):** a "simple mode" that generates BPMN from a status table, an engine-neutral namespace for every attribute, a second engine adapter, Camunda 8 (a remote, async engine that needs a licence to self-host), a JQL-style query language, and sprints.

**Rejected alternatives:**
- **A custom status-machine engine** (a table of statuses and transitions instead of BPMN): BPMN plus the existing visual editor already gives visual design, and adds automation that a status table cannot express. BPMN's real cost is moving open items onto a new workflow version, not editing the diagram; 20.4 handles that.
- **Flowable CMMN (case management):** more powerful for long-lived cases, but much heavier to learn and model, which works against a simple tracker.

## 20.2 Data ownership: what is copied between engine and database

| Data | Owner | Copy |
|---|---|---|
| Item fields, custom field values, comments, attachments | `wf_item` + `wf_comments` + `wf_attachments` | None. The engine only receives `itemId` |
| Current status | Engine (token position) | `wf_item.status_key`, `status_name`, `status_category`, written by the task-created listener in the same transaction |
| Assignee | `wf_item.assignee` | None. Engine tasks stay unassigned because "my items" queries `wf_item` |
| Who may transition out of a status | `flowable:candidateGroups` on the status task in the model | None. Read from the cached model, not from runtime tasks |
| Status history | `wf_item_transition` (from, to, flow, actor, at, workflow version, reason) | The engine keeps its own history, but the platform never reads it (keeps the facade engine-neutral) |
| Workflow definition | Engine repository | The parsed `WorkflowDescriptor` (statuses, transitions) is cached in memory and not persisted |
| Process variables | `itemId`, plus a transient `transition` | Routing on item data calls a bean, e.g. `${items.field(itemId, 'severity')}`, instead of copying fields into variables |

## 20.3 Tracker profile and editor guardrails

The *tracker profile* is this project's own concept: the subset of BPMN that the tracker accepts, plus the `wfp:statusCategory` attribute. It is a restricted subset in the same spirit as the conformance sub-classes defined in the BPMN 2.0 spec. `wfp:` is a custom XML namespace (`http://wfp.com/schema/tracker`); the BPMN XSD allows foreign attributes, so engines ignore it.

A **status** is a user task, or a subprocess that carries `wfp:statusCategory` (a *status subprocess*). User tasks inside a status subprocess are steps of that one status, not statuses of their own, and need no category.

The adapter parses the XML into an engine-neutral `WorkflowGraph` (nodes and flows). The validator and `describe()` both work on that graph, so all the rules live in one place, in Java.

**Rules:**
1. Only these elements are allowed: start event, end event, user task, service task, exclusive gateway, subprocess, message event subprocess, boundary timer. Parallel gateways are allowed only inside a status subprocess.
2. Exactly one start event. Following automatic nodes (service tasks, gateways) from it must reach exactly one status, which is the item's initial status.
3. Every user task has a `wfp:statusCategory`, and status names are unique. At least one status is `DONE`.
4. Every outgoing flow of a gateway that follows a status is named, and names are unique per gateway.
5. Every status is reachable from the start, and an end event is reachable from every status.
6. A status has exactly one outgoing flow: to the exclusive gateway that holds its transitions, or straight to the next node. Two flows out of a task would be an implicit parallel split. (Added 2026-10-01, #36.)

**Editor (`frontend/packages/bpmn-editor`):**
- The palette and context pad offer only the profile's elements.
- The properties panel adds a status-category dropdown on user tasks, requires a name on transition flows, and hides the generated condition.
- Live validation: on change (debounced) the editor calls `POST /api/workflow/workflows/validate`, which returns `[{elementId, rule, message}]`. The editor shows markers and overlays on the offending elements.
- Saving a draft with errors is allowed. Deploy runs the same validator and rejects the workflow on any error.

## 20.4 Moving open items to a new workflow version

On deploy the admin chooses to **keep** open items on their current version (the default) or **move** them. Before confirming, a dry run shows how many items fall into each outcome below.

For each open item, choose the target status:
1. The current status, if its element id exists in the new version.
2. Otherwise, walk `wf_item_transition` backwards (most recent first) and take the first status whose id exists in the new version.
3. Otherwise, the new version's initial status (a reset).

Then cancel the old run and call `startAt(version, itemId, statusId)`. On Flowable that is a normal start followed by `ChangeActivityStateBuilder.moveActivityIdTo`; on Camunda 7 or Operaton it is `startBeforeActivity`. Record a transition row with reason `WORKFLOW_UPGRADE` and publish an audit event. Item fields are untouched, and entry automation does not re-fire.

This depends on element ids staying stable across edits. The editor preserves them when a status is renamed.

**Limits (accepted):** only wait states move, so automation in flight at the time is dropped. Progress inside a parallel status subprocess restarts. Boundary timers restart.

The move runs as an async job, one transaction per item, and reports the counts for each outcome.

## 20.5 Engine facade

```java
interface WorkflowEngine {
    WorkflowGraph parse(String bpmnXml);
    DeployedWorkflow deploy(String tenantId, String key, String bpmnXml);
    WorkflowDescriptor describe(String workflowVersionId);
    WorkflowRun start(String tenantId, String workflowVersionId, UUID itemId);
    WorkflowRun startAt(String tenantId, String workflowVersionId, UUID itemId, String statusId);
    void transition(String runId, String transitionId, String actor);
    void cancel(String runId, String reason);
}
// engine → tracker callbacks: statusEntered, automationFailed
```

The adapter is today's Flowable code from `DeploymentService`, `ProcessService`, `TaskService`, `ProcessHistoryService` and `FlowableEventListener` moved behind this interface. An ArchUnit test fails the build if `org.flowable` is imported outside the adapter package. That also fixes the `Deployment` type leaking into `DeploymentController`.

**Engine swap notes:**
- **Camunda 7 and its community forks (Operaton, CIB seven):** the closest fit. They share Flowable's Activiti heritage, have a near-identical API, support tenant ids, and have `startBeforeActivity`. Camunda 7 Community Edition has reached end of life, so the forks are the open-source option.
- **Activiti:** feasible, but check its start-at-activity support before relying on 20.4.
- **Camunda 8 (Zeebe):** out of scope. It is a remote, async engine with no runtime queries in the same process, and it needs a licence to self-host in production.

## 20.6 Delivery order

| # | Slice | Done when |
|---|---|---|
| 20.a | **Spike:** `WorkflowGraph` parser, profile validator, `describe()`, tested against sample BPMN files, no UI | Valid and invalid samples give the expected statuses, transitions and rule violations |
| 20.b | Extract the facade (refactor only). Pair it with the codebase simplification review | Existing tests and BDD scenarios green; ArchUnit rule in place |
| 20.c | Item model: `wf_item`, `jsonb` custom fields, key sequence, `wf_item_transition`, REST API, `item.*` events with field diffs | Create → transition → close works end to end through the gateway |
| 20.d | Editor guardrails and the validate endpoint | Invalid elements are marked live; deploy is refused with errors |
| 20.e | User portal: item list with a structured filter builder (field / operator / value → SQL over fixed and `jsonb` fields), board by status category, item detail with transition buttons and history | Playwright flow: create, move across the board, close, reopen |
| 20.f | Moving items to a new version, with dry run | Integration test covers all three outcomes (same status, walked back, reset) |
| 20.g | Fold `custom-fields-service` into `workflow-service`; update Docker, Helm, gateway, CI and `docs/architecture/` | 4 backend services; CI green |

## 20.7 Input for the codebase simplification review

- Process, task and history endpoints that item endpoints replace.
- Whether `ProcessMetadata` overlaps with projects.
- DTOs exposing engine concepts such as `processDefinitionId` in `key:version:uuid` form.
- `custom-fields-service` (to be retired in 20.g).
- Declared-but-unused events (`field.*`, `process.sla.breached`). SLA becomes a boundary timer publishing `item.sla.breached`.

## 20.8 After 20.g (not scheduled)

- Saved filters.
- Watchers and @mentions, which read `NotificationPreference` before notifying (closes that open gap).
- Links between items (*blocks*, *relates to*).
- Attachment upload and download (gives `Attachment` its missing endpoint).
- **Request portal:** forms that create items in a service-desk project, SLA timers per request type, and "my requests" for the people who submit them.

