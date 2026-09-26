---
title: task.created
tags:
  - event
  - reference
  - routing-key/task
type: reference
source: libs/wfp-events/src/main/java/com/wfp/events/TaskCreatedEvent.java
routing_key: task.created
java_class: TaskCreatedEvent
---

> Routing key `task.created` · class `TaskCreatedEvent`

| | |
|---|---|
| Published by | `FlowableEventListener` in [[Workflow Service]] |
| Consumed by | [[Notification Service]] · [[Audit Service]] |
| Exchange | `wfp.events` (topic) |

## Payload

Inherited from `BaseEvent`: `eventId`, `eventType`, `tenantId`, `userId`, `timestamp`.

| Field | Type | Notes |
|---|---|---|
| `taskId` | `String` |  |
| `taskName` | `String` |  |
| `taskDefinitionKey` | `String` | BPMN element id |
| `processInstanceId` | `String` |  |
| `processDefinitionKey` | `String` |  |
| `assignee` | `String` | may be null for unclaimed group tasks |
| `dueDate` | `Instant` |  |
| `priority` | `int` |  |

## Notes

Engine-originated. `assignee` is null for tasks offered to a group rather than a person; `WorkflowEventListener` skips notification creation in that case.

## See also

[[Event Catalog]] · [[Event System]] · [[wfp-events]]
