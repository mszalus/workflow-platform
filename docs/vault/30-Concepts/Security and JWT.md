---
title: Security and JWT
tags:
  - concept
  - security
  - auth
  - keycloak
type: concept
source: libs/wfp-security, services/gateway
---

Keycloak 25 is the sole identity provider. Every service is an OAuth2 **resource server**;
none of them holds a session.

## Flow

1. The browser runs an OIDC Authorization Code flow against Keycloak
   (realm `workflow-platform`) from [[Admin Portal]] or [[User Portal]].
2. The SPA sends the access token as `Authorization: Bearer …` to the [[API Gateway]].
3. The gateway validates the signature against the Keycloak JWK Set.
4. `JwtTenantConverter` maps realm roles to Spring authorities and reads `tenant_id`.
5. `TenantHeaderFilter` propagates the tenant downstream — see [[Multi-Tenancy]].
6. Each backend service independently re-validates the JWT. **The gateway is not a
   trust boundary the services rely on** — they do not accept unauthenticated traffic
   even if reached directly.

## Public endpoints

Whitelisted in `SecurityConfig`, no token required:

`/actuator/health` · `/actuator/info` · `/v3/api-docs/**` · `/swagger-ui/**`

## Gotcha: the gateway has no database

[[wfp-security]] drags in Spring Data JPA. `GatewayApplication` must exclude
`DataSourceAutoConfiguration` and `HibernateJpaAutoConfiguration` or it will not boot.
See [[Known Pitfalls]].

## See also

[[Admin — Keycloak Administration]] · [[API Gateway]] · [[User — Login]]
