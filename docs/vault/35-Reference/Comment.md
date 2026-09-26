---
title: Comment
tags:
  - entity
  - reference
  - schema/workflow
type: reference
source: services/workflow-service
table: wf_comments
schema: workflow
---

> Table `wf_comments` · schema `workflow` · owned by [[Workflow Service]]

## Columns

| Field | Type | Notes |
|---|---|---|
| `id` | `UUID` | PK |
| `processInstanceId` | `String` | not null |
| `taskId` | `String` | nullable — comment may target the instance, not a task |
| `userId` | `String` | not null |
| `content` | `TEXT` | not null |
| `createdAt` | `Instant` | not null, immutable |
| `tenantId` | `String` | not null |

## Tenancy

Declares **`@Filter` only** — the `@FilterDef` lives on another entity in this service. See [[Multi-Tenancy]].

## Notes

Application-level comments, deliberately **not** [[Flowable Engine|Flowable]] native comments — they are queryable with plain JPA and survive engine history cleanup.

## See also

[[Data Model ERD]] · [[Entity Reference]] · [[Workflow Service]]
