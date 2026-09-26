---
title: task.delegated
tags:
  - event
  - reference
  - routing-key/task
type: reference
source: libs/wfp-events/src/main/java/com/wfp/events/TaskDelegatedEvent.java
routing_key: task.delegated
java_class: TaskDelegatedEvent
---

> Routing key `task.delegated` · class `TaskDelegatedEvent`

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
| `delegatedFrom` | `String` |  |
| `delegatedTo` | `String` |  |
| `comment` | `String` | reason supplied by the delegator |

## Notes

Published explicitly by `TaskService.delegate`.

## See also

[[Event Catalog]] · [[Event System]] · [[wfp-events]]
