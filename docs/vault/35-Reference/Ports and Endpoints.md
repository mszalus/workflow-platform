---
title: Ports and Endpoints
tags:
  - reference
  - ops
  - ports
type: reference
source: docker/docker-compose.yml
---
[[Operations MOC]] › **Ports and Endpoints**

Host ports as mapped by Docker Compose. Note PostgreSQL is deliberately **5433** on the
host to avoid colliding with a local install — see [[Known Pitfalls]].

## Application

| Service | Container | Host | URL |
|---|---|---|---|
| [[Admin Portal]] | 8080 | **5173** | http://localhost:5173 |
| [[User Portal]] | 8080 | **5174** | http://localhost:5174 |
| [[API Gateway]] | 8080 | **9080** | http://localhost:9080 |
| [[Workflow Service]] | 8081 | 8081 | http://localhost:8081/actuator/health |
| [[Custom Fields Service]] | 8082 | 8082 | http://localhost:8082/actuator/health |
| [[Notification Service]] | 8083 | 8083 | http://localhost:8083/actuator/health |
| [[Audit Service]] | 8084 | 8084 | http://localhost:8084/actuator/health |

## Infrastructure

| Component | Host port | URL / note |
|---|---|---|
| PostgreSQL 16 | **5433** | 5432 inside the network |
| [[Event System|RabbitMQ]] | 5672 | AMQP |
| RabbitMQ management | 15672 | http://localhost:15672 |
| [[Security and JWT|Keycloak]] 25 | **8180** | http://localhost:8180 |

## Observability

| Component | Host port | URL / note |
|---|---|---|
| OTel Collector | 4317 / 4318 | gRPC / HTTP OTLP ingest |
| OTel Collector metrics | 8888 | scraped by [[Observability Stack|Prometheus]] |
| Tempo | 3200 | trace query API |
| Prometheus | 9090 | http://localhost:9090 |
| Grafana | 3000 | http://localhost:3000 |

## Schemas in the shared PostgreSQL instance

`workflow` · `custom_fields` · `notification` · `audit` · `keycloak`

Created by `docker/init-db.sql`.

## See also

[[Docker Compose Stack]] · [[Observability Stack]] · [[Quick Start]]


---

**Running and shipping** — ← [[Observability Stack]]

> [!abstract]- All notes in this set
> [[Prerequisites]]
> [[Quick Start]]
> [[Local Development]]
> [[Tech Stack]]
> [[Repository Layout]]
> [[Build Commands]]
> [[Docker Compose Stack]]
> [[CI Pipeline]]
> [[Helm and Kubernetes]]
> [[Observability Stack]]
