---
title: AuditEntry
tags:
  - entity
  - reference
  - schema/audit
type: reference
source: services/audit-service
table: audit_entry
schema: audit
---

> Table `audit_entry` · schema `audit` · owned by [[Audit Service]]

## Columns

| Field | Type | Notes |
|---|---|---|
| `id` | `UUID` | PK |
| `eventType` | `String` | not null — the routing key |
| `entityType` | `String` | not null — e.g. `task`, `process` |
| `entityId` | `String` | not null |
| `userId` | `String` | who caused it |
| `tenantId` | `String` | not null |
| `timestamp` | `Instant` | not null |
| `details` | `TEXT` | serialized event payload |
| `sourceService` | `String` | which service emitted it |

## Tenancy

Declares the single **`@FilterDef`** for this persistence unit, plus `@Filter`. See [[Multi-Tenancy]].

## Notes

One row per event on the bus — the `wfp.audit` queue binds `#`, so this table is the complete history of everything the platform has published. Queried through `AuditEntrySpecification`.

## See also

[[Data Model ERD]] · [[Entity Reference]] · [[Audit Service]]
