---
title: ProcessMetadata
tags:
  - entity
  - reference
  - schema/workflow
type: reference
source: services/workflow-service
table: wf_process_metadata
schema: workflow
---

> Table `wf_process_metadata` · schema `workflow` · owned by [[Workflow Service]]

## Columns

| Field | Type | Notes |
|---|---|---|
| `id` | `UUID` | PK |
| `processDefinitionKey` | `String` | not null |
| `tenantId` | `String` | not null |
| `description` | `String` |  |
| `category` | `String` |  |
| `iconUrl` | `String` |  |
| `createdAt` | `Instant` | not null, immutable |
| `updatedAt` | `Instant` | not null |

## Tenancy

Declares the single **`@FilterDef`** for this persistence unit, plus `@Filter`. See [[Multi-Tenancy]].

## Notes

Presentation metadata for a process definition — the things BPMN XML has no place for. Keyed by `processDefinitionKey`, so it survives redeployment of a new version.

Holds the single `@FilterDef` for the `workflow` persistence unit.

## See also

[[Data Model ERD]] · [[Entity Reference]] · [[Workflow Service]]
