---
title: User — Troubleshooting
tags:
  - manual
  - end-user
type: manual
source: docs/user-manual.md
---
[[Manuals MOC]] › [[User Portal]] › **User — Troubleshooting**

| Issue | Solution |
|-------|----------|
| "Loading..." stuck on screen | Check that all backend services are running. Verify gateway health at `http://localhost:9080/actuator/health` |
| Redirected to login repeatedly | Your JWT token may have expired. Clear browser cookies and log in again |
| No processes available to start | Ask an administrator to deploy a BPMN process definition |
| Tasks not appearing | Verify you're logged in as the correct user. Tasks are filtered by assignee |
| Notifications not appearing | Notifications are created asynchronously via [[Event System|RabbitMQ]]. Allow a few seconds for delivery |


---

**User manual** — ← [[User — Multi-Tenant Isolation]]

> [!abstract]- All notes in this set
> [[User — Getting Started]]
> [[User — Login]]
> [[User — Dashboard]]
> [[User — Task Inbox]]
> [[User — Completing a Task]]
> [[User — Starting a New Process]]
> [[User — My Processes]]
> [[User — Notifications]]
> [[User — Multi-Tenant Isolation]]
