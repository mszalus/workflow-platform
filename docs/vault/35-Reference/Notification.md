---
title: Notification
tags:
  - entity
  - reference
  - schema/notification
type: reference
source: services/notification-service
table: notification
schema: notification
---

> Table `notification` · schema `notification` · owned by [[Notification Service]]

## Columns

| Field | Type | Notes |
|---|---|---|
| `id` | `UUID` | PK |
| `userId` | `String` | not null — recipient |
| `tenantId` | `String` | not null |
| `title` | `String` | not null |
| `message` | `TEXT` |  |
| `type` | `NotificationType` | enum, not null |
| `read` | `boolean` | not null, default false |
| `referenceId` | `String` | task or process instance id |
| `referenceType` | `String` | which of the two |
| `createdAt` | `Instant` | immutable |

## Tenancy

Declares the single **`@FilterDef`** for this persistence unit, plus `@Filter`. See [[Multi-Tenancy]].

## Notes

Created only by `WorkflowEventListener` from a consumed event — never by an API call. Holds the `@FilterDef` for the `notification` persistence unit.

## See also

[[Data Model ERD]] · [[Entity Reference]] · [[Notification Service]]
