---
title: FieldSchema
tags:
  - entity
  - reference
  - schema/custom_fields
type: reference
source: services/custom-fields-service
table: field_schema
schema: custom_fields
---

> Table `field_schema` · schema `custom_fields` · owned by [[Custom Fields Service]]

## Columns

| Field | Type | Notes |
|---|---|---|
| `id` | `UUID` | PK |
| `processDefinitionKey` | `String` | not null |
| `fieldKey` | `String` | not null — the variable name |
| `label` | `String` | not null |
| `fieldType` | `FieldType` | enum, not null |
| `required` | `boolean` | not null |
| `sortOrder` | `int` | render order |
| `defaultValue` | `String` |  |
| `placeholder` | `String` |  |
| `validationRegex` | `String` | client and server validation |
| `tenantId` | `String` | not null |
| `createdAt` | `Instant` |  |
| `updatedAt` | `Instant` |  |
| `options` | `List<FieldOption>` | cascade ALL, orphan removal |

## Tenancy

Declares the single **`@FilterDef`** for this persistence unit, plus `@Filter`. See [[Multi-Tenancy]].

## Notes

One user-defined field on one process definition. Holds the `@FilterDef` for the `custom_fields` persistence unit. Types are listed in [[Enumerations]].

## See also

[[Data Model ERD]] · [[Entity Reference]] · [[Custom Fields Service]]
