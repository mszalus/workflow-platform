---
title: task.completed
tags:
  - event
  - reference
  - routing-key/task
type: reference
source: libs/wfp-events/src/main/java/com/wfp/events/TaskCompletedEvent.java
routing_key: task.completed
java_class: TaskCompletedEvent
---

> Routing key `task.completed` · class `TaskCompletedEvent`

| | |
|---|---|
| Published by | `TaskService` in [[Workflow Service]] |
| Consumed by | [[Notification Service]] · [[Audit Service]] |
| Exchange | `wfp.events` (topic) |

## Payload

Inherited from `BaseEvent`: `eventId`, `eventType`, `tenantId`, `userId`, `timestamp`.

| Field | Type | Notes |
|---|---|---|
| `taskId` | `String` |  |
| `taskName` | `String` |  |
| `processInstanceId` | `String` |  |
| `processDefinitionKey` | `String` |  |
| `completedBy` | `String` |  |
| `outcome` | `Map<String, Object>` | variables submitted with the completion |
| `durationMillis` | `long` | time the task was open |

## Notes

Published explicitly by `TaskService.complete`. The `outcome` map carries the form variables, including values bound to [[Custom Fields Service]] schemas.

## See also

[[Event Catalog]] · [[Event System]] · [[wfp-events]]
