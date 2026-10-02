# CLAUDE.md — Workflow Platform

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.


## Working Agreements

- **Verify before claiming done**: Build, run tests, and start the application if infra is available. Don't commit untested code.
- **Keep PRs green**: Check that `./gradlew build` and `npm run typecheck` pass before marking a PR ready. `main` is protected, so a red required check blocks the merge.
- **Commit granularity**: Logical commits — one per feature/fix, not one per file.
- **One item at a time**: When the user approves a list of items, do them in order: one branch and PR per item, then ask the user to merge and wait for the merge before starting the next item from the updated `main`. Don't create worktrees or extra branches to work on items in parallel. A background session, which Claude Code requires to work in a worktree, uses one worktree for the whole list.
- **Minimal comments**: Write as few comments as possible. Express intent through method and variable names, and extract helpers instead of writing explanatory comments. Only comment when something genuinely cannot be expressed through naming (e.g., a non-obvious external library workaround).
- **Simple over clever**: Build the simplest design that meets today's need. Avoid speculative abstractions, extra layers, configurability nobody asked for, and a second implementation of an interface "just in case". When proposing a design, say which parts could be cut and default to cutting them.
- **Standards before custom builds**: Prefer an existing standard or tool the stack already has (BPMN plus the bpmn-js editor, Flowable, Keycloak, PostgreSQL `jsonb`) over writing our own engine, DSL or framework.
- **Thin facades only at replaceable boundaries**: Put a third-party engine that may be swapped (Flowable → Camunda 7, Operaton, Activiti) behind a small interface written in domain terms, with one adapter. Don't wrap stable libraries.
- **Fewer moving parts**: Don't add a service, module, queue or copy of data when an existing one can own it. Data that must change together belongs in one service and one transaction.
- **Simplify as you plan**: When planning, collect what could be simplified or removed. Section 20.7 of [docs/design/work-item-tracker.md](docs/design/work-item-tracker.md) lists the input for the codebase simplification review.



End of Behavioral guidelines section. The rest of this README describes the Workflow Platform project.



## Where Things Live

| Need | Look in |
|------|---------|
| Open work | GitHub Issues: `gh issue list`. Milestones are the phases; the `process` label marks engineering-process work |
| How we work | [docs/engineering-process.md](docs/engineering-process.md) |
| Architecture and the reasons behind it | [docs/architecture/](docs/architecture/): `concepts.md`, C4 diagrams, data model |
| Designs for upcoming work | [docs/design/](docs/design/) |
| Completed work and past decisions | [docs/project-history.md](docs/project-history.md). Read it only when a task needs that background |
| Running the stack, API overview | [README.md](README.md) |
| User and admin guides | `docs/user-manual.md`, `docs/admin-manual.md` |

## Engineering Process

Sessions, branches and merging, gates, review and subagent delegation are described in [docs/engineering-process.md](docs/engineering-process.md). In short:

- **`main`** is protected and the only long-lived branch. Work lands through short-lived branches and PRs that pass the required checks. Only the human merges.
- A PR that finishes an issue says `Closes #<n>` in its description.
- Delegate mechanical work to the subagents in `.claude/agents/` (`test-runner`, `ci-triager`). Keep design and tenancy or security changes in the main session.

## Project Overview

Multi-tenant BPMN workflow platform. Users design workflows visually (bpmn-js), deploy them, and end users complete tasks through a task inbox. Every action is audited, custom fields can be attached to any process, and notifications are delivered in real-time.

Services: `app` (8081: Flowable, custom fields, notifications and audit), reached through the portals' nginx `/api` proxy. The React apps are in `frontend/apps/`, the local stack in `docker/docker-compose.yml`. Versions: Spring Boot in `buildSrc/build.gradle`, frontend in `frontend/package.json`.

## Build Commands

### Backend (requires JDK 21)
```bash
./gradlew build                                    # compile + test all modules
./gradlew :services:app:bootJar -x test  # build single service JAR
./gradlew :services:app:test           # test single service
```

### Frontend
```bash
cd frontend
npm ci                                              # install deps (clean)
npm run build                                       # build all packages + apps (order matters)
npm run typecheck --workspaces --if-present          # TypeScript type check
npm run dev:admin                                   # dev server for admin-portal
npm run dev:user                                    # dev server for user-portal
```

### Docker
```bash
docker compose -f docker/docker-compose.yml build              # build all images
docker compose -f docker/docker-compose.yml up -d              # start full stack
docker compose -f docker/docker-compose.yml logs <service>     # view logs
docker compose -f docker/docker-compose.yml config --quiet     # validate compose file
```

### Helm
```bash
helm lint helm/charts/<chart-name>/                            # lint single chart
helm dependency update helm/workflow-platform/                 # pull bitnami deps
helm install wfp helm/workflow-platform/ -f helm/workflow-platform/values-local.yaml
```

## Tenancy

Every request is tenant-scoped: validated JWT `tenant_id` claim → `TenantInterceptor` in each service → `TenantContext` → Hibernate `tenantFilter` and the `tenantId` argument on every Flowable call. A query or engine call without the tenant is a cross-tenant data leak. Read the Multi-Tenancy section of [docs/architecture/concepts.md](docs/architecture/concepts.md) before changing any of these layers.

## Known Pitfalls

1. **Gradle requires all project directories**: `settings.gradle` includes all modules — Dockerfiles must copy the entire `services/` directory, not just the target service
2. **Port conflicts**: Local PostgreSQL on 5432 conflicts with Docker. Docker compose maps PG to `5433` externally
3. **Hibernate tenant filter**: `tenantFilter` is defined once, at package level in `com/wfp/workflow/entity/package-info.java`. Entities declare `@Filter` only; a second `@FilterDef` with that name throws at boot
4. **Flowable + H2 tests**: Requires `MODE=LEGACY` in the JDBC URL, not `MODE=PostgreSQL`
5. **Frontend build order**: `shared-ui` → `bpmn-editor` → apps (apps depend on packages)
6. **CI gradlew permission**: The `gradlew` file must have execute permission in git (`git update-index --chmod=+x gradlew`)
7. **Gradle daemon JDK**: Gradle 9.2 cannot run on JDK 26+. `gradle/gradle-daemon-jvm.properties` pins the daemon to Java 21, which Gradle picks from locally installed JDKs whatever `JAVA_HOME` says

## Testing

- Backend integration tests run the full Spring context on H2 (`@SpringBootTest`, `MODE=LEGACY`)
- Test config: `src/test/resources/application-test.yml` with `SPRING_PROFILES_ACTIVE=test`
- Authenticated endpoint tests use Spring Security's `jwt()` request post-processor with a `tenant_id` claim; service-layer tests use `TenantContext.runAs`
- Frontend: Vitest with React Testing Library on jsdom. One config, `frontend/vitest.config.ts`, runs every `*.test.ts(x)` under `packages/*/src` and `apps/*/src`: `cd frontend && npm test` (`npm run test:coverage` for the coverage report, as CI does)
- Acceptance: Cucumber BDD in `tests/bdd-acceptance` and Playwright in `e2e/` (`npm test`), both run in CI against a fresh Docker stack. `npm run screenshots` in `e2e/` refreshes `docs/screenshots/`

## MCP Servers

- **Playwright** (`@playwright/mcp`) — browser automation for E2E testing. Use for verifying Keycloak, frontend portals, and service health endpoints.
