---
title: Known Pitfalls
tags:
  - concept
  - gotchas
  - troubleshooting
type: concept
source: CLAUDE.md
---

The failure modes this codebase actually hits. Each links to the note that explains why.

| # | Trap | Fix | Context |
|---|---|---|---|
| 1 | Docker build fails evaluating Gradle projects | Copy the **entire** `services/` dir into the build context — `settings.gradle` includes all modules | [[Build System]] |
| 2 | PostgreSQL port conflict on 5432 | Compose maps PG to **5433** on the host | [[Ports and Endpoints]] |
| 3 | Boot fails: duplicate filter definition | Only **one** `@FilterDef` per persistence unit; other entities use `@Filter` only | [[Multi-Tenancy]] |
| 4 | RabbitMQ listener receives `byte[]` | Add a `Jackson2JsonMessageConverter` bean to the RabbitMQ config | [[Event System]] |
| 5 | Flowable schema creation fails on H2 | Use `MODE=LEGACY`, not `MODE=PostgreSQL`, in the test JDBC URL | [[Flowable Engine]] |
| 6 | Test context fails without a broker | Inject `@Nullable RabbitTemplate` in `EventPublisher` | [[Testing Strategy]] |
| 7 | Frontend build fails on missing types | Build order: `shared-ui` → `bpmn-editor` → apps | [[Frontend Architecture]] |
| 8 | CI: `./gradlew` permission denied | `git update-index --chmod=+x gradlew` | [[CI Pipeline]] |
| 9 | Gateway will not start, wants a `DataSource` | Exclude `DataSourceAutoConfiguration` + `HibernateJpaAutoConfiguration` | [[API Gateway]] |
| 10 | Partially overridden gateway routes in Docker | Use named service URL env vars, not indexed `..._ROUTES_N_URI` | [[Gateway Routing]] |

## See also

[[Admin — Troubleshooting]] · [[User — Troubleshooting]] · [[GCP VM — Troubleshooting]]
