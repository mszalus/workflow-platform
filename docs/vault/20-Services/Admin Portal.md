---
title: Admin Portal
tags:
  - frontend
  - react
type: service
source: frontend/apps/admin-portal
---

> React 18 + TypeScript + Vite SPA for process designers and platform admins.
> Served by nginx in Docker; host port `5173`.

## Pages

| Page | Purpose |
|---|---|
| `Dashboard.tsx` | Deployment counts, recent activity |
| `ProcessDesigner.tsx` | [[bpmn-editor|bpmn-js]] canvas — draw, import, export, deploy |
| `ProcessList.tsx` | Deployed definitions, delete |
| `CustomFieldEditor.tsx` | Author [[FieldSchema]] rows per process definition |
| `AuditLog.tsx` | Query the [[Audit Service]] trail |

Shared pieces come from [[shared-ui]]; the modeller from [[bpmn-editor]].

## See also

[[Frontend Architecture]] · [[Admin — Admin Portal]] · [[Admin — Process Designer]] · [[Screenshot Gallery]]
