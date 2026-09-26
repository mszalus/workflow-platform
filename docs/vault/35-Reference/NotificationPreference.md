---
title: NotificationPreference
tags:
  - entity
  - reference
  - schema/notification
type: reference
source: services/notification-service
table: notification_preference
schema: notification
---

> Table `notification_preference` · schema `notification` · owned by [[Notification Service]]

## Columns

| Field | Type | Notes |
|---|---|---|
| `id` | `UUID` | PK |
| `userId` | `String` | not null |
| `tenantId` | `String` | not null |
| `eventType` | `String` | not null |
| `emailEnabled` | `boolean` | default true |
| `inAppEnabled` | `boolean` | default true |

## Tenancy

Declares **`@Filter` only** — the `@FilterDef` lives on another entity in this service. See [[Multi-Tenancy]].

## Notes

> [!warning] Defined but not enforced
> Nothing consults this table before creating a [[Notification]]. Wiring it into `WorkflowEventListener` is a planned enhancement; email delivery does not exist at all.

## See also

[[Data Model ERD]] · [[Entity Reference]] · [[Notification Service]]
