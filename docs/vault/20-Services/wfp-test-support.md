---
title: wfp-test-support
tags:
  - library
  - backend
type: library
source: libs/wfp-test-support
---

Test-only fixtures shared across service test suites.

| Class | Role |
|---|---|
| `JwtTestHelper` | Mints mock JWTs with tenant and role claims for MockMvc tests |
| `TenantTestHelper` | Sets and clears `TenantContext` around service-layer tests |
| `TestContainersConfig` | Shared PostgreSQL + [[Event System|RabbitMQ]] [[Testing Strategy|Testcontainers]] definitions |

See [[Testing Strategy]].
