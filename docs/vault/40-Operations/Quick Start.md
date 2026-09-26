---
title: Quick Start
tags:
  - ops
type: reference
source: README.md
---
[[Operations MOC]] › **Quick Start**

```bash
# Clone the repository
git clone https://github.com/mszalus/workflow-platform.git
cd workflow-platform

# Build and start all services
docker compose -f docker/docker-compose.yml build
docker compose -f docker/docker-compose.yml up -d

# Check all containers are running
docker compose -f docker/docker-compose.yml ps
```

Once running, the services are available at:

| Service | URL |
|---------|-----|
| [[API Gateway]] | http://localhost:9080 |
| [[Security and JWT|Keycloak]] Admin | http://localhost:8180 (admin/admin) |
| [[Event System|RabbitMQ]] Management | http://localhost:15672 (wfp/wfp_secret) |
| [[Admin Portal]] | http://localhost:5173 |
| [[User Portal]] | http://localhost:5174 |
| [[Workflow Service]] (direct) | http://localhost:8081 |
| [[Custom Fields Service]] (direct) | http://localhost:8082 |
| [[Notification Service]] (direct) | http://localhost:8083 |
| [[Audit Service]] (direct) | http://localhost:8084 |
| PostgreSQL | localhost:5433 (wfp/wfp_secret) |


---

**Running and shipping** — ← [[Prerequisites]] · [[Local Development]] →

> [!abstract]- All notes in this set
> [[Prerequisites]]
> [[Local Development]]
> [[Tech Stack]]
> [[Repository Layout]]
> [[Build Commands]]
> [[Docker Compose Stack]]
> [[CI Pipeline]]
> [[Helm and Kubernetes]]
> [[Observability Stack]]
> [[Ports and Endpoints]]
