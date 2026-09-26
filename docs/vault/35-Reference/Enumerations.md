---
title: Enumerations
tags:
  - reference
  - enum
type: reference
source: services/*/entity/*Type.java
---

## FieldType

Drives which control `DynamicFieldForm.tsx` renders for a [[FieldSchema]].

| Value | Rendered as |
|---|---|
| `TEXT` | single-line input |
| `TEXTAREA` | multi-line input |
| `NUMBER` | numeric input |
| `DATE` | date picker |
| `DATETIME` | date + time picker |
| `BOOLEAN` | checkbox |
| `DROPDOWN` | single select, needs [[FieldOption]] rows |
| `MULTI_SELECT` | multi select, needs [[FieldOption]] rows |
| `FILE` | file picker — depends on [[Attachment]], which has no endpoint yet |
| `USER_PICKER` | user lookup |

All values are stored as `TEXT` in [[FieldValue]] regardless of type.

## NotificationType

| Value | Produced by |
|---|---|
| `TASK_ASSIGNED` | [[task.created]], [[task.assigned]] |
| `TASK_COMPLETED` | [[task.completed]] |
| `PROCESS_COMPLETED` | [[process.completed]] |
| `SLA_BREACH` | nothing yet — `process.sla.breached` has no publisher |
| `INFO` | reserved for manual or system notices |

## See also

[[Entity Reference]] · [[Event Catalog]] · [[Custom Fields Service]]
