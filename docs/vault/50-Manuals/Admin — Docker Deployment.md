---
title: Admin — Docker Deployment
tags:
  - manual
  - admin
type: manual
source: docs/admin-manual.md
---
[[Manuals MOC]] › [[Admin Portal]] › **Admin — Docker Deployment**

## Starting the Full Stack

```bash
# Build all images
docker compose -f docker/docker-compose.yml build

# Start all containers
docker compose -f docker/docker-compose.yml up -d

# Check container status
docker compose -f docker/docker-compose.yml ps
```

## Container Overview

| Container          | Image              | Port  | Description            |
|-------------------|--------------------|-------|------------------------|
| wfp-postgres      | postgres:16-alpine | 5433  | Database (all schemas) |
| wfp-rabbitmq      | rabbitmq:3.13-management | 5672, 15672 | Message broker |
| wfp-keycloak      | keycloak/keycloak:25 | 8180 | Identity provider      |
| wfp-gateway       | (built)            | 9080  | API gateway            |
| wfp-workflow      | (built)            | 8081  | Workflow engine        |
| wfp-custom-fields | (built)            | 8082  | Custom fields service  |
| wfp-notification  | (built)            | 8083  | Notification service   |
| wfp-audit         | (built)            | 8084  | Audit service          |
| wfp-admin-portal  | (built)            | 5173  | Admin frontend         |
| wfp-user-portal   | (built)            | 5174  | User frontend          |

## Startup Order

The docker-compose file defines dependencies:

1. **PostgreSQL** starts first (health check: `pg_isready`)
2. **[[Event System|RabbitMQ]]** starts first (health check: `rabbitmq-diagnostics -q ping`)
3. **[[Security and JWT|Keycloak]]** starts after PostgreSQL is healthy
4. **Backend services** start after PostgreSQL and RabbitMQ are healthy
5. **Frontend containers** start after gateway is running

## Viewing Logs

```bash
# All services
docker compose -f docker/docker-compose.yml logs -f

# Single service
docker compose -f docker/docker-compose.yml logs -f workflow-service

# Last 100 lines
docker compose -f docker/docker-compose.yml logs --tail=100 workflow-service
```

## Database Schemas

PostgreSQL uses separate schemas per service (created by `docker/init-db.sql`):

| Schema          | Service              |
|----------------|----------------------|
| `workflow`     | [[Workflow Service|workflow-service]]     |
| `custom_fields`| [[Custom Fields Service|custom-fields-service]]|
| `notification` | [[Notification Service|notification-service]] |
| `audit`        | [[Audit Service|audit-service]]        |
| `keycloak`     | Keycloak             |

## Environment Variables

Backend service URIs are configured via environment variables in `docker-compose.yml`:

| Variable                | Default                | Description               |
|------------------------|------------------------|---------------------------|
| `WORKFLOW_SERVICE_URL` | `http://workflow:8081` | Workflow service URI      |
| `CUSTOM_FIELDS_SERVICE_URL` | `http://custom-fields:8082` | Custom fields URI |
| `NOTIFICATION_SERVICE_URL` | `http://notification:8083` | Notification URI    |
| `AUDIT_SERVICE_URL`    | `http://audit:8084`    | Audit service URI         |

---


---

**Admin manual** — ← [[Admin — RabbitMQ Monitoring]] · [[Admin — Kubernetes Deployment]] →

> [!abstract]- All notes in this set
> [[Admin — Overview]]
> [[Admin — Architecture]]
> [[Admin — Admin Portal]]
> [[Admin — Process Designer]]
> [[Admin — Managing Process Definitions]]
> [[Admin — Custom Field Schemas]]
> [[Admin — Audit Log]]
> [[Admin — Keycloak Administration]]
> [[Admin — RabbitMQ Monitoring]]
> [[Admin — Kubernetes Deployment]]
> [[Admin — Troubleshooting]]
