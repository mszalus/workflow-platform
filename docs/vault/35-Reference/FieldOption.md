---
title: FieldOption
tags:
  - entity
  - reference
  - schema/custom_fields
type: reference
source: services/custom-fields-service
table: field_option
schema: custom_fields
---

> Table `field_option` · schema `custom_fields` · owned by [[Custom Fields Service]]

## Columns

| Field | Type | Notes |
|---|---|---|
| `id` | `UUID` | PK |
| `fieldSchema` | `FieldSchema` | `@ManyToOne(LAZY)`, not null |
| `label` | `String` | not null |
| `value` | `String` | not null |
| `sortOrder` | `int` |  |

## Tenancy

**No tenant column.** Filtered indirectly through its parent. See [[Multi-Tenancy]].

## Notes

Choices for a `DROPDOWN` or `MULTI_SELECT` [[FieldSchema]].

> [!info] No `tenant_id` column
> The only entity without one. It is reachable exclusively through its parent schema, which is already tenant-filtered — see [[Multi-Tenancy]].

## See also

[[Data Model ERD]] · [[Entity Reference]] · [[Custom Fields Service]]
