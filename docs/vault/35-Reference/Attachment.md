---
title: Attachment
tags:
  - entity
  - reference
  - schema/workflow
type: reference
source: services/workflow-service
table: wf_attachments
schema: workflow
---

> Table `wf_attachments` · schema `workflow` · owned by [[Workflow Service]]

## Columns

| Field | Type | Notes |
|---|---|---|
| `id` | `UUID` | PK |
| `processInstanceId` | `String` | not null |
| `taskId` | `String` | nullable |
| `fileName` | `String` | not null |
| `contentType` | `String` | not null |
| `fileSize` | `Long` | not null |
| `storageKey` | `String` | not null — pointer into blob storage |
| `uploadedBy` | `String` | not null |
| `createdAt` | `Instant` | not null, immutable |
| `tenantId` | `String` | not null |

## Tenancy

Declares **`@Filter` only** — the `@FilterDef` lives on another entity in this service. See [[Multi-Tenancy]].

## Notes

> [!warning] No controller yet
> The entity and `AttachmentRepository` exist, but no REST endpoint exposes them. Upload and download are unimplemented; `storageKey` anticipates object storage.

## See also

[[Data Model ERD]] · [[Entity Reference]] · [[Workflow Service]]
