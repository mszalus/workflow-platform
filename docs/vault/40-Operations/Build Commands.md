---
title: Build Commands
tags:
  - ops
  - build
type: reference
source: CLAUDE.md
---
[[Operations MOC]] › **Build Commands**

## Backend — requires JDK 21

```bash
./gradlew build                                       # compile + test all modules
./gradlew :services:workflow-service:bootJar -x test  # single service JAR
./gradlew :services:workflow-service:test             # single service tests
```

## Frontend

```bash
cd frontend
npm ci                                       # clean install
npm run build                                # shared-ui → bpmn-editor → apps
npm run typecheck --workspaces --if-present
npm run dev:admin                            # admin-portal dev server
npm run dev:user                             # user-portal dev server
```

Build order is load-bearing — see [[Frontend Architecture]].

## Docker

```bash
docker compose -f docker/docker-compose.yml build
docker compose -f docker/docker-compose.yml up -d
docker compose -f docker/docker-compose.yml logs <service>
docker compose -f docker/docker-compose.yml config --quiet   # validate
```

## Helm

```bash
helm lint helm/charts/<chart-name>/
helm dependency update helm/workflow-platform/
helm install wfp helm/workflow-platform/ -f helm/workflow-platform/values-local.yaml
```

## Before pushing

`./gradlew build` **and** `npm run typecheck` must both pass — see [[CI Pipeline]].

## See also

[[Build System]] · [[Quick Start]] · [[Local Development]] · [[Docker Compose Stack]]


---

**Running and shipping** — ← [[Repository Layout]] · [[Docker Compose Stack]] →

> [!abstract]- All notes in this set
> [[Prerequisites]]
> [[Quick Start]]
> [[Local Development]]
> [[Tech Stack]]
> [[Repository Layout]]
> [[Docker Compose Stack]]
> [[CI Pipeline]]
> [[Helm and Kubernetes]]
> [[Observability Stack]]
> [[Ports and Endpoints]]
