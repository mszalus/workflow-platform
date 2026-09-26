---
title: Custom Fields Service
tags:
  - service
  - backend
type: service
source: services/custom-fields-service
---

> Attaches arbitrary user-defined fields to any process definition, and stores their
> values per process instance. Neither publishes nor consumes events today.

| | |
|---|---|
| Port | `8082` |
| Module | `services/custom-fields-service` |
| Schema | `custom_fields` |
| Gateway path | `/api/fields/**` → `/api/**` |

## Endpoints

| Controller | Base path | Operations |
|---|---|---|
| `FieldSchemaController` | `/api/schemas` | `POST` create · `GET /{id}` · `GET ?processDefinitionKey=` · `DELETE /{id}` |
| `FieldValueController` | `/api/values` | `POST` save (bulk) · `GET ?processInstanceId=` |

## Model

A [[FieldSchema]] defines one field on one `processDefinitionKey`; a `DROPDOWN` or
`MULTI_SELECT` schema owns a list of [[FieldOption]]; a [[FieldValue]] is the answer
captured for one process instance.

```
FieldSchema 1 ──< FieldOption
FieldSchema 1 ──< FieldValue   (by fieldSchemaId, no FK constraint)
```

Field types are listed in [[Enumerations]].

## Cross-service coupling

`FieldValue.processInstanceId` and `FieldSchema.processDefinitionKey` point at [[Flowable Engine|Flowable]]
rows in the `workflow` schema with **no foreign key** — the schemas are independent.
See [[Data Model ERD]].

The end-user form is rendered by `DynamicFieldForm.tsx` in the [[User Portal]]; the
schemas are authored in `CustomFieldEditor.tsx` in the [[Admin Portal]].

## See also

[[Admin — Custom Field Schemas]] · [[API Endpoint Catalog]]
