---
title: Gateway Routing
tags:
  - concept
  - gateway
  - routing
type: concept
source: services/gateway/src/main/resources/application.yml
---

Four routes, two of which rewrite the path. The asymmetry is deliberate:
[[Workflow Service]] and [[Custom Fields Service]] expose generic `/api/**` paths that
would collide, so the gateway namespaces them; [[Notification Service]] and
[[Audit Service]] already expose distinct prefixes and pass through untouched.

| External path | Target | Rewrite |
|---|---|---|
| `/api/workflow/**` | [[Workflow Service|workflow-service]]:8081 | `RewritePath=/api/workflow(?:/(?<segment>.*))?$, /api/${segment}` |
| `/api/fields/**` | [[Custom Fields Service|custom-fields-service]]:8082 | `RewritePath=/api/fields(?:/(?<segment>.*))?$, /api/${segment}` |
| `/api/notifications/**` | [[Notification Service|notification-service]]:8083 | pass-through |
| `/api/audit/**` | [[Audit Service|audit-service]]:8084 | pass-through |

So `/api/workflow/tasks` reaches the backend as `/api/tasks`, but
`/api/notifications/unread-count` arrives verbatim.

## Overriding URIs in Docker

Compose sets `SPRING_CLOUD_GATEWAY_MVC_ROUTES_N_URI` per route index. The project also
defines named vars (`WORKFLOW_SERVICE_URL`, `CUSTOM_FIELDS_SERVICE_URL`,
`NOTIFICATION_SERVICE_URL`, `AUDIT_SERVICE_URL`) referenced from `application.yml`,
because indexed env vars silently drop the rest of a route definition when partially
overridden.

> [!tip] Frontend path bug class
> A portal calling `/api/tasks` instead of `/api/workflow/tasks` gets a 404 from the
> gateway, not from the service. See [[Frontend API Path Bug Fix]].

## See also

[[C4 L3 API Gateway]] · [[API Endpoint Catalog]] · [[Ports and Endpoints]]
