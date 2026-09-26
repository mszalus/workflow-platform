---
title: API Gateway
tags:
  - service
  - backend
  - gateway
type: service
source: services/gateway
---

> Thin routing + security edge. **No database.**

| | |
|---|---|
| Port | `8080` (container) / `9080` (host) |
| Module | `services/gateway` |
| Framework | Spring Cloud Gateway **MVC** (servlet, not reactive) |
| Schema | — none — |

## Responsibilities

1. Validate the JWT signature against the [[Security and JWT|Keycloak]] JWK Set.
2. Extract `tenant_id` from the token and forward it as `X-Tenant-Id` — see [[Multi-Tenancy]].
3. Rewrite and route `/api/**` paths to the four backend services — see [[Gateway Routing]].
4. Expose a public `/actuator/health` for probes.

## Classes

| Class | Role |
|---|---|
| `GatewayApplication` | Boot entrypoint; excludes `DataSourceAutoConfiguration` and `HibernateJpaAutoConfiguration` |
| `GatewaySecurityConfig` | OAuth2 resource server, public endpoint whitelist |
| `CorsConfig` | Allowed origins for the two portals |
| `TenantHeaderFilter` | Reads the `tenant_id` claim, sets `X-Tenant-Id` downstream |

## Why it excludes JPA autoconfiguration

The gateway depends on [[wfp-security]], which pulls Spring Data JPA transitively. Without
the exclusion, Boot fails at startup looking for a `DataSource` the gateway does not have.
Recorded in [[Known Pitfalls]].

## See also

[[C4 L3 API Gateway]] · [[Gateway Routing]] · [[API Endpoint Catalog]] · [[Ports and Endpoints]]
