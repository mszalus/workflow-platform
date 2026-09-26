---
title: Event Catalog
tags:
  - event
  - reference
  - moc
type: reference
source: libs/wfp-events
---

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
