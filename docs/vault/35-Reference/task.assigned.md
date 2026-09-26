---
title: task.assigned
tags:
  - event
  - reference
  - routing-key/task
type: reference
source: libs/wfp-events/src/main/java/com/wfp/events/TaskAssignedEvent.java
routing_key: task.assigned
java_class: TaskAssignedEvent
---

> Routing key `task.assigned` · class `TaskAssignedEvent`

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
| `processInstanceId` | `String` |  |
| `assignee` | `String` | new owner |
| `previousAssignee` | `String` | null on first assignment |

## Notes

Fires on claim and on reassignment. Produces a `TASK_ASSIGNED` notification.

## See also

[[Event Catalog]] · [[Event System]] · [[wfp-events]]
