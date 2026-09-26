---
title: Docker Compose Stack
tags:
  - ops
  - docker
type: reference
source: docker/docker-compose.yml
---
[[Operations MOC]] › **Docker Compose Stack**

The full local stack: 5 backend containers, 2 frontends, 3 infrastructure containers and
4 observability containers.

## Containers

| Group | Containers |
|---|---|
| Data | `wfp-postgres` (PG 16), `wfp-rabbitmq` (3.13-management), `wfp-keycloak` (25.0.6) |
| Backend | `wfp-gateway`, `wfp-workflow`, `wfp-custom-fields`, `wfp-notification`, `wfp-audit` |
| Frontend | `wfp-admin-portal`, `wfp-user-portal` (nginx) |
| Observability | `wfp-otel-collector`, `wfp-tempo`, `wfp-prometheus`, `wfp-grafana` |

Port map: [[Ports and Endpoints]].

## Startup order

PostgreSQL and [[Event System|RabbitMQ]] come up first with healthchecks, then [[Security and JWT|Keycloak]] (which needs its
own schema in PG), then the backend services, then the portals. `init-db.sql` creates all
five schemas on first boot of the PG volume.

> [!warning] Schemas are created once
> `init-db.sql` runs only when the PostgreSQL data volume is empty. Adding a schema later
> means either running the DDL by hand or removing the volume.

## Build context gotcha

Each backend Dockerfile copies the **entire** `services/` directory, because
`settings.gradle` includes every module and [[Build System|Gradle]] evaluates all of them. See
[[Known Pitfalls]].

## See also

[[Quick Start]] · [[Admin — Docker Deployment]] · [[Deployment — Docker Compose]] · [[Build Commands]]


---

**Running and shipping** — ← [[Build Commands]] · [[CI Pipeline]] →

> [!abstract]- All notes in this set
> [[Prerequisites]]
> [[Quick Start]]
> [[Local Development]]
> [[Tech Stack]]
> [[Repository Layout]]
> [[Build Commands]]
> [[CI Pipeline]]
> [[Helm and Kubernetes]]
> [[Observability Stack]]
> [[Ports and Endpoints]]
