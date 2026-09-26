---
title: Notification Service
tags:
  - service
  - backend
  - events
type: service
source: services/notification-service
---

> Entirely event-driven. Nothing creates a notification synchronously — every row
> arrives over RabbitMQ.

| | |
|---|---|
| Port | `8083` |
| Module | `services/notification-service` |
| Schema | `notification` |
| Gateway path | `/api/notifications/**` (pass-through, no rewrite) |
| Queue | `wfp.notification`, binds `task.*` and `process.completed` |

## Endpoints

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/notifications` | Paginated list for the caller |
| `GET` | `/api/notifications/unread-count` | `{ "count": n }` |
| `PUT` | `/api/notifications/mark-read` | Body: array of notification UUIDs |
| `PUT` | `/api/notifications/mark-all-read` | All for the caller |

## Event to notification mapping

| Incoming event | NotificationType | Target user |
|---|---|---|
| [[task.created]] | `TASK_ASSIGNED` | task assignee |
| [[task.assigned]] | `TASK_ASSIGNED` | task assignee |
| [[task.completed]] | `TASK_COMPLETED` | completer |
| [[process.completed]] | `PROCESS_COMPLETED` | process initiator |

`WorkflowEventListener` tolerates a null `userId` (unassigned tasks) by skipping the row.

## Entities

[[Notification]] · [[NotificationPreference]]

> [!warning] Not yet wired
> `NotificationPreference` exists and is queryable but the listener does **not** consult it
> before creating a notification. Email delivery is likewise unimplemented.

## See also

[[C4 L3 Notification Service]] · [[Event System]] · [[User — Notifications]] · [[Enumerations]]
