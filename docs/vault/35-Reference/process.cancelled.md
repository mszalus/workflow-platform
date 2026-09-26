---
title: process.cancelled
tags:
  - event
  - reference
  - routing-key/process
type: reference
source: libs/wfp-events/src/main/java/com/wfp/events/ProcessCancelledEvent.java
routing_key: process.cancelled
java_class: ProcessCancelledEvent
---

> Routing key `process.cancelled` · class `ProcessCancelledEvent`

| | |
|---|---|
| Published by | `ProcessService` in [[Workflow Service]] |
| Consumed by | [[Audit Service]] |
| Exchange | `wfp.events` (topic) |

## Payload

Inherited from `BaseEvent`: `eventId`, `eventType`, `tenantId`, `userId`, `timestamp`.

| Field | Type | Notes |
|---|---|---|
| `processInstanceId` | `String` |  |
| `processDefinitionKey` | `String` |  |
| `reason` | `String` | defaults to `Cancelled by user` |

## Notes

Published by `DELETE /api/processes/{id}`. Audited but never notified.

## See also

[[Event Catalog]] · [[Event System]] · [[wfp-events]]
