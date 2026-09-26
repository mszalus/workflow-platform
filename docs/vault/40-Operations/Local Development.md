---
title: Local Development
tags:
  - ops
type: reference
source: README.md
---
[[Operations MOC]] › **Local Development**

## Backend

```bash
# Build all modules
./gradlew build

# Build a single service JAR (skip tests)
./gradlew :services:workflow-service:bootJar -x test

# Run tests for a single service
./gradlew :services:audit-service:test

# Start a service locally (requires PG, RabbitMQ, Keycloak running)
./gradlew :services:workflow-service:bootRun
```

## Frontend

```bash
cd frontend

# Install dependencies
npm ci

# Start admin portal dev server
npm run dev:admin

# Start user portal dev server
npm run dev:user

# Build all packages and apps
npm run build

# TypeScript type check
npm run typecheck --workspaces --if-present
```

## Infrastructure Only

To run just the backing services (for local development of backend/frontend):

```bash
docker compose -f docker/docker-compose.yml up -d postgres rabbitmq keycloak
```


---

**Running and shipping** — ← [[Quick Start]] · [[Tech Stack]] →

> [!abstract]- All notes in this set
> [[Prerequisites]]
> [[Quick Start]]
> [[Tech Stack]]
> [[Repository Layout]]
> [[Build Commands]]
> [[Docker Compose Stack]]
> [[CI Pipeline]]
> [[Helm and Kubernetes]]
> [[Observability Stack]]
> [[Ports and Endpoints]]
