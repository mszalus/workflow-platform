---
title: Testing Strategy
tags:
  - concept
  - testing
  - quality
type: concept
source: libs/wfp-test-support, e2e/, tests/
---

| Layer | Tooling | Scope |
|---|---|---|
| Backend unit | JUnit 5 | services and mappers |
| Backend integration | **Testcontainers** (PostgreSQL + [[Event System|RabbitMQ]]) | repositories, listeners, full Spring context |
| Backend web | MockMvc + `JwtTestHelper` | authenticated endpoints |
| BDD acceptance | Cucumber-style features under `tests/` | cross-service behaviour |
| E2E | Playwright (`e2e/`) | both portals against the running stack |
| Frontend | `tsc --noEmit` only | no unit test framework yet |

Integration tests activate `SPRING_PROFILES_ACTIVE=test` and read
`src/test/resources/application-test.yml`.

## Fixtures

[[wfp-test-support]] supplies `JwtTestHelper` (mock tokens with tenant and role claims),
`TenantTestHelper` (sets and clears `TenantContext`) and `TestContainersConfig`.

## Traps

- Flowable on H2 requires `MODE=LEGACY` — see [[Known Pitfalls]].
- `EventPublisher` takes a `@Nullable RabbitTemplate` so contexts without RabbitMQ start.
- React component tests must pre-seed the QueryClient cache; a `useQuery` + `useEffect`
  pair otherwise loops forever.

## See also

[[CI Pipeline]] · [[Step 07 — Playwright E2E Tests]] · [[Step 11 — Comprehensive E2E Testing and Bug Fixes]]
