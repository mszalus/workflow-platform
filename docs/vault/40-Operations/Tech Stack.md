---
title: Tech Stack
tags:
  - ops
type: reference
source: README.md
---
[[Operations MOC]] › **Tech Stack**

| Layer | Technology |
|-------|-----------|
| BPMN Engine | [[Flowable Engine|Flowable]] 7.1.0 |
| Backend | Java 21, Spring Boot 3.3.5, Spring Cloud 2023.0.3 |
| [[API Gateway]] | Spring Cloud Gateway MVC |
| Database | PostgreSQL 16 (schema-per-service) |
| Messaging | [[Event System|RabbitMQ]] 3.13 (topic exchange) |
| Identity | [[Security and JWT|Keycloak]] 25 (OIDC/JWT, Organizations for tenants) |
| Frontend | React 18, TypeScript, Vite, [[bpmn-editor|bpmn-js]] |
| Build | [[Build System|Gradle]] 9.2 (Groovy DSL), npm workspaces |
| Deployment | Docker Compose, Helm/Kubernetes |


---

**Running and shipping** — ← [[Local Development]] · [[Repository Layout]] →

> [!abstract]- All notes in this set
> [[Prerequisites]]
> [[Quick Start]]
> [[Local Development]]
> [[Repository Layout]]
> [[Build Commands]]
> [[Docker Compose Stack]]
> [[CI Pipeline]]
> [[Helm and Kubernetes]]
> [[Observability Stack]]
> [[Ports and Endpoints]]
