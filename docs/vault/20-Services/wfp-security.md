---
title: wfp-security
tags:
  - library
  - backend
type: library
source: libs/wfp-security
---

The [[Multi-Tenancy|multi-tenancy]] and authentication machinery, shared by every backend service — and,
awkwardly, by the [[API Gateway]], which is why the gateway must exclude JPA
autoconfiguration.

| Class | Role |
|---|---|
| `SecurityConfig` | OAuth2 resource server, JWT decoder, public endpoint whitelist |
| `JwtTenantConverter` | Maps [[Security and JWT|Keycloak]] realm roles to authorities, reads `tenant_id` |
| `TenantContext` | `ThreadLocal<String>` holding the current tenant |
| `TenantInterceptor` | Reads `X-Tenant-Id` on each request, populates `TenantContext` |
| `TenantFilterAspect` | AOP: enables the Hibernate `tenantFilter` per request |
| `TenantHibernateFilter` | Filter definition plumbing |

See [[Multi-Tenancy]] and [[Security and JWT]].
