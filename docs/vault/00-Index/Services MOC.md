---
title: Services MOC
tags:
  - moc
  - services
type: moc
source: services/, frontend/
---

## Backend

| Service | Port | Schema | Publishes | Consumes |
|---|---|---|---|---|
| [[API Gateway]] | 9080→8080 | — | — | — |
| [[Workflow Service]] | 8081 | `workflow` | all 7 events | — |
| [[Custom Fields Service]] | 8082 | `custom_fields` | — | — |
| [[Notification Service]] | 8083 | `notification` | — | `task.*`, `process.completed` |
| [[Audit Service]] | 8084 | `audit` | — | `#` (everything) |

## Frontend

[[Admin Portal]] (5173) · [[User Portal]] (5174)

## Shared libraries

| Backend | Frontend |
|---|---|
| [[wfp-common]] | [[shared-ui]] |
| [[wfp-events]] | [[bpmn-editor]] |
| [[wfp-security]] | |
| [[wfp-test-support]] | |

## See also

[[Ports and Endpoints]] · [[API Endpoint Catalog]] · [[Repository Layout]]
