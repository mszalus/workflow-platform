---
title: process.started
tags:
  - event
  - reference
  - routing-key/process
type: reference
source: libs/wfp-events/src/main/java/com/wfp/events/ProcessStartedEvent.java
routing_key: process.started
java_class: ProcessStartedEvent
---

> Routing key `process.started` · class `ProcessStartedEvent`

| | |
|---|---|
| Published by | `ProcessService` in [[Workflow Service]] |
| Consumed by | [[Audit Service]] |
| Exchange | `wfp.events` (topic) |

## Payload

Inherited from `BaseEvent`: `eventId`, `eventType`, `tenantId`, `userId`, `timestamp`.

| Field | Type | Notes |
|---|---|---|
| `processInstanceId` | `String` | [[Flowable Engine|Flowable]] instance id |
| `processDefinitionId` | `String` | versioned definition id |
| `processDefinitionKey` | `String` | stable key across versions |
| `processName` | `String` | display name |
| `businessKey` | `String` | caller-supplied correlation key |
| `variables` | `Map<String, Object>` | initial process variables |

## Notes

Published explicitly by `ProcessService` when the API starts an instance. Not consumed by [[Notification Service]] — the queue binds `task.*` and `process.completed` only.

## See also

[[Event Catalog]] · [[Event System]] · [[wfp-events]]
