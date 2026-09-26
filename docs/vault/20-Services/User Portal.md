---
title: User Portal
tags:
  - frontend
  - react
type: service
source: frontend/apps/user-portal
---

> React 18 + TypeScript + Vite SPA for day-to-day workflow participants.
> Served by nginx in Docker; host port `5174`.

## Pages

| Page | Purpose |
|---|---|
| `Dashboard.tsx` | Task counts, unread notifications |
| `TaskInbox.tsx` | My tasks + tasks available to claim |
| `TaskDetail.tsx` | Task form, comments, complete and delegate |
| `StartProcess.tsx` | Pick a definition and start it |
| `MyProcesses.tsx` | Instances the user started |
| `Notifications.tsx` | List, mark read |

`DynamicFieldForm.tsx` renders the [[Custom Fields Service]] schema for a process at
start and at task completion.

## See also

[[Frontend Architecture]] · [[User — Task Inbox]] · [[User — Completing a Task]] · [[Screenshot Gallery]]
