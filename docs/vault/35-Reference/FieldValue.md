---
title: FieldValue
tags:
  - entity
  - reference
  - schema/custom_fields
type: reference
source: services/custom-fields-service
table: field_value
schema: custom_fields
---

> Table `field_value` · schema `custom_fields` · owned by [[Custom Fields Service]]

## Columns

| Field | Type | Notes |
|---|---|---|
| `id` | `UUID` | PK |
| `fieldSchemaId` | `UUID` | not null — **no FK constraint** |
| `processInstanceId` | `String` | not null — points into the `workflow` schema |
| `value` | `TEXT` | every type serialized as text |
| `tenantId` | `String` | not null |
| `createdAt` | `Instant` |  |
| `updatedAt` | `Instant` |  |

## Tenancy

Declares **`@Filter` only** — the `@FilterDef` lives on another entity in this service. See [[Multi-Tenancy]].

## Notes

The captured answer for one field on one process instance. Note that it references `FieldSchema` by raw UUID rather than a JPA association, and `processInstanceId` crosses a schema boundary with no referential integrity. See [[Data Model ERD]].

## See also

[[Data Model ERD]] · [[Entity Reference]] · [[Custom Fields Service]]
