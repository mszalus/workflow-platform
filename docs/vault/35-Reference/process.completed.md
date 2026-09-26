---
title: process.completed
tags:
  - event
  - reference
  - routing-key/process
type: reference
source: libs/wfp-events/src/main/java/com/wfp/events/ProcessCompletedEvent.java
routing_key: process.completed
java_class: ProcessCompletedEvent
---

> Routing key `process.completed` · class `ProcessCompletedEvent`

| | |
|---|---|
| Published by | `FlowableEventListener` in [[Workflow Service]] |
| Consumed by | [[Notification Service]] · [[Audit Service]] |
| Exchange | `wfp.events` (topic) |

## Payload

Inherited from `BaseEvent`: `eventId`, `eventType`, `tenantId`, `userId`, `timestamp`.

| Field | Type | Notes |
|---|---|---|
| `processInstanceId` | `String` |  |
| `processDefinitionKey` | `String` |  |
| `processName` | `String` |  |
| `durationMillis` | `long` | wall-clock lifetime of the instance |

## Notes

Engine-originated: the process ends because the engine reached an end event, not because anyone called an API. Becomes a `PROCESS_COMPLETED` notification for the initiator.

## See also

[[Event Catalog]] · [[Event System]] · [[wfp-events]]
