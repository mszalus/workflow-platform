---
title: Audit Service
tags:
  - service
  - backend
  - events
type: service
source: services/audit-service
---

> Write-only sink for every event on the bus, plus a query API over the trail.

| | |
|---|---|
| Port | `8084` |
| Module | `services/audit-service` |
| Schema | `audit` |
| Gateway path | `/api/audit/**` (pass-through, no rewrite) |
| Queue | `wfp.audit`, binds `#` — **all** routing keys |

## Endpoints

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/audit` | Paginated, filterable query over [[AuditEntry]] |

Filtering is built with `AuditEntrySpecification` (JPA Criteria) over entity type,
entity id, user, event type and time range.

## Why it binds `#`

Audit is deliberately the catch-all consumer: any new routing key added to the
[[Event System]] is captured without touching this service. Contrast with
[[Notification Service]], whose bindings must be widened explicitly.

## Entities

[[AuditEntry]] — carries `sourceService`, so the trail records which service emitted the row.

## See also

[[Admin — Audit Log]] · [[Event Catalog]]
