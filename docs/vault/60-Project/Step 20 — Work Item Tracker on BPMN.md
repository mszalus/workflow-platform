---
title: Step 20 — Work Item Tracker on BPMN
tags:
  - project
  - plan
  - status/open
type: plan
source: PLAN.md
status: open
---
[[Project MOC]] › **Step 20 — Work Item Tracker on BPMN**

**Goal:** Turn the platform into a simple issue tracker. A **work item** has a few fixed fields and any number of custom fields. Its lifecycle is a BPMN process drawn in the existing editor. The BPMN engine sits behind a small facade, so [[Flowable Engine|Flowable]] can later be swapped for Camunda 7, Operaton or Activiti.

## 20.1 Decisions

| Topic | Decision | Why |
|---|---|---|
| Record of truth | New `wf_item` table. Every item owns exactly one process run | Items must be listable, searchable and editable without going through the engine |
| Where it lives | In `workflow-service`, in the same schema and transaction as Flowable | The status copy and the engine state commit together, so no cross-service sync or events are needed to stay consistent |
| One model | Every process run belongs to an item. "Start process" becomes "create item"; the task inbox becomes "my items" | Avoids two parallel concepts (bare processes and items) |
| Fixed fields | `key` (`PROJ-123`, per-project sequence), `project`, `type`, `title`, `description`, `status`, `priority`, `assignee`, `reporter`, `created_at`, `updated_at` | Everything else is a custom field |
| Custom field values | `jsonb` column on `wf_item`: GIN index for filters, expression indexes for sorted fields | Lists filter and sort on fixed and custom fields in one SQL query |
| Custom field definitions | Move `FieldSchema`/`FieldOption` into `workflow-service` and scope them by project + item type. Retire `custom-fields-service` | Values and definitions sit next to the items; one service fewer |
| Status | A BPMN **user task** (a wait state) marked with `wfp:statusCategory` = `TODO` \| `IN_PROGRESS` \| `DONE` | The category drives board columns and "resolved" semantics |
| Transitions | The **named outgoing flows** of the exclusive gateway directly after a status. The user picks one, and the editor generates the condition `${transition == '<flowId>'}`. A status with a single outgoing flow has one transition | Multiple paths = multiple buttons. Gateways after service tasks remain automatic routing |
| Transitions from any status | An interrupting message event subprocess (e.g. *Cancel*) | Avoids drawing an arrow from every status |
| Reopen | *Done* is a user task with a *Reopen* flow. The process ends only at an end event (*Closed*) | A finished process cannot be resumed |
| Parallel work | Only inside a subprocess that counts as one status (e.g. *In Approval* with two parallel approvals) | An item is always in exactly one status |
| Engine facade | A `WorkflowEngine` interface in tracker terms, with a single Flowable adapter. Stored BPMN keeps the `flowable:*` attributes | Swapping to Camunda 7 or Operaton means a new adapter plus a one-time `flowable:` → `camunda:` rewrite of stored XML |

**Deferred (judged over-engineering for now):** a "simple mode" that generates BPMN from a status table, an engine-neutral namespace for every attribute, a second engine adapter, Camunda 8 (a remote, async engine that needs a licence to self-host), a JQL-style query language, and sprints.

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

The *tracker profile* is this project's own concept: the subset of BPMN that the tracker accepts, plus the `wfp:statusCategory` attribute. It is a restricted subset in the same spirit as the conformance sub-classes defined in the BPMN 2.0 spec. `wfp:` is a custom XML namespace; the BPMN XSD allows foreign attributes, so engines ignore it.

The adapter parses the XML into an engine-neutral `WorkflowGraph` (nodes and flows). The validator and `describe()` both work on that graph, so all the rules live in one place, in Java.

**Rules:**
1. Only these elements are allowed: start event, end event, user task, service task, exclusive gateway, subprocess, message event subprocess, boundary timer. Parallel gateways are allowed only inside a status subprocess.
2. Exactly one start event. The first status reached from it is the item's initial status.
3. Every user task has a `wfp:statusCategory`, and status names are unique. At least one status is `DONE`.
4. Every outgoing flow of a gateway that follows a status is named, and names are unique per gateway.
5. Every status is reachable from the start, and an end event is reachable from every status.

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

## 20.6 Delivery order

| # | Slice | Done when |
|---|---|---|
| 20.a | **Spike:** `WorkflowGraph` parser, profile validator, `describe()`, tested against sample BPMN files, no UI | Valid and invalid samples give the expected statuses, transitions and rule violations |
| 20.b | Extract the facade (refactor only). Pair it with the codebase simplification review | Existing tests and BDD scenarios green; ArchUnit rule in place |
| 20.c | Item model: `wf_item`, `jsonb` custom fields, key sequence, `wf_item_transition`, REST API, `item.*` events with field diffs | Create → transition → close works end to end through the gateway |
| 20.d | Editor guardrails and the validate endpoint | Invalid elements are marked live; deploy is refused with errors |
| 20.e | User portal: filterable item list, board by status category, item detail with transition buttons and history | [[Testing Strategy|Playwright]] flow: create, move across the board, close, reopen |
| 20.f | Moving items to a new version, with dry run | Integration test covers all three outcomes (same status, walked back, reset) |
| 20.g | Fold `custom-fields-service` into `workflow-service`; update Docker, [[Helm and Kubernetes|Helm]], gateway and CI; regenerate the vault | 4 backend services; `run_all.sh` reports `broken: 0` |

## 20.7 Input for the codebase simplification review

- Process, task and history endpoints that item endpoints replace.
- Whether `ProcessMetadata` overlaps with projects.
- DTOs exposing engine concepts such as `processDefinitionId` in `key:version:uuid` form.
- `custom-fields-service` (to be retired in 20.g).
- Declared-but-unused events (`field.*`, `process.sla.breached`). SLA becomes a boundary timer publishing `item.sla.breached`.

---


---

**Plan** — ← [[Step 19 — GCP Observability Readiness no deployment]] · [[Commit Strategy]] →

> [!abstract]- All notes in this set
> [[Active Tasks]]
> [[Project Context]]
> [[Step 01 — Fix Backend Dockerfiles]]
> [[Step 02 — Runtime test custom-fields-service and notification-service]]
> [[Step 03 — Gateway routing test]]
> [[Step 04 — Docker full-stack build and E2E]]
> [[Step 05 — README documentation]]
> [[Step 09 — Documentation User Manual and Admin Manual]]
> [[Step 10 — BPMN Import Export]]
> [[Step 06 — Helm deployment]]
> [[CI Pipeline Fixes]]
> [[Step 07 — Playwright E2E Tests]]
> [[Step 08 — Flowable BPMN Editor Extensions]]
> [[Frontend API Path Bug Fix]]
> [[Step 11 — Comprehensive E2E Testing and Bug Fixes]]
> [[Verification Log]]
> [[Step 12 — Architecture Diagrams and Data Model]]
> [[Step 13 — SDLC Improvements]]
> [[Step 14 — Observability and BDD Acceptance Tests]]
> [[Step 15 — Local Observability Verification]]
> [[Step 16 — GCP Infrastructure Terraform Helm Preparation]]
> [[Step 17 — GCP Deployment and Acceptance Testing]]
> [[GCP Cost Estimate]]
> [[Step 18 — Release and Rollback Strategy]]
> [[Step 19 — GCP Observability Readiness no deployment]]
> [[Commit Strategy]]
