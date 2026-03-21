# Admin Manual — Workflow Platform

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Admin Portal](#admin-portal)
4. [Process Designer](#process-designer)
5. [Deploying Processes](#deploying-processes)
6. [Managing Process Definitions](#managing-process-definitions)
7. [Custom Field Schemas](#custom-field-schemas)
8. [Audit Log](#audit-log)
9. [Keycloak Administration](#keycloak-administration)
10. [RabbitMQ Monitoring](#rabbitmq-monitoring)
11. [Docker Deployment](#docker-deployment)
12. [Kubernetes Deployment](#kubernetes-deployment)
13. [Troubleshooting](#troubleshooting)

---

## Overview

The Workflow Platform Admin Portal provides tools for workflow administrators to:

- Design BPMN workflow processes using a visual editor
- Deploy and manage process definitions
- Configure custom fields for processes
- View audit logs of all platform activity
- Monitor infrastructure components

**URL:** `http://localhost:5173` (local development)

---

## Architecture

```
┌─────────────┐     ┌─────────────┐
│ Admin Portal│     │ User Portal │
│  :5173      │     │  :5174      │
└──────┬──────┘     └──────┬──────┘
       │ nginx /api proxy  │
       └────────┬──────────┘
                │
         ┌──────▼──────┐
         │   Gateway   │
         │   :9080     │
         └──────┬──────┘
                │
    ┌───────────┼───────────────┬──────────────┐
    ▼           ▼               ▼              ▼
┌────────┐ ┌────────────┐ ┌────────────┐ ┌─────────┐
│Workflow│ │Custom Fields│ │Notification│ │  Audit  │
│ :8081  │ │   :8082    │ │   :8083    │ │  :8084  │
└───┬────┘ └─────┬──────┘ └─────┬──────┘ └────┬────┘
    │            │              │              │
    └────────────┴──────┬───────┴──────────────┘
                        │
              ┌─────────┼──────────┐
              ▼         ▼          ▼
         ┌────────┐ ┌────────┐ ┌────────┐
         │Postgres│ │RabbitMQ│ │Keycloak│
         │ :5433  │ │ :5672  │ │ :8180  │
         └────────┘ └────────┘ └────────┘
```

### Gateway Routing

| Frontend Path          | Gateway Route           | Backend Service        |
|-----------------------|-------------------------|------------------------|
| `/api/workflow/**`    | `RewritePath → /api/**` | workflow-service:8081  |
| `/api/fields/**`      | `RewritePath → /api/**` | custom-fields:8082    |
| `/api/notifications/**` | Pass-through          | notification:8083     |
| `/api/audit/**`       | Pass-through            | audit-service:8084    |

### Event System

Services communicate asynchronously via RabbitMQ:

- **Exchange:** `wfp.events` (topic)
- **Routing Keys:** `task.created`, `task.assigned`, `task.completed`, `process.started`, `process.completed`
- **Queues:**
  - `wfp.notification` — binds `task.*` + `process.completed` → creates user notifications
  - `wfp.audit` — binds `#` (all events) → records audit entries

---

## Admin Portal

### Login

Navigate to `http://localhost:5173`. You'll be redirected to Keycloak for authentication.

Use an admin account (e.g., `admin-a` / `password`).

### Dashboard

The admin dashboard shows:
- **Deployed Processes** — count of deployed process definitions
- **Active Instances** — count of running process instances
- **Custom Field Schemas** — count of configured field schemas

---

## Process Designer

Navigate to **Process Designer** in the sidebar to create BPMN workflow diagrams.

### BPMN Editor

The editor is based on [bpmn-js](https://bpmn.io/toolkit/bpmn-js/) with Flowable extensions. It provides:

- **Visual canvas** — drag-and-drop BPMN elements (tasks, events, gateways, sequence flows)
- **Properties panel** (right sidebar) — configure element properties
- **Palette** (left sidebar) — BPMN element toolbox

### Supported BPMN Elements

| Element        | Description                                    |
|----------------|------------------------------------------------|
| Start Event    | Process entry point                            |
| End Event      | Process exit point                             |
| User Task      | Human task assigned to a user or group         |
| Service Task   | Automated task (Java class, expression, delegate) |
| Exclusive Gateway | Decision point (XOR split/join)             |
| Parallel Gateway  | Parallel split/join                          |
| Sequence Flow  | Connection between elements                    |

### Flowable Properties

The properties panel includes Flowable-specific configuration:

#### User Task Properties
| Property         | Description                                         |
|-----------------|-----------------------------------------------------|
| Assignee        | User assigned to the task (e.g., `${initiator}`)    |
| Candidate Users | Comma-separated list of candidate users             |
| Candidate Groups| Comma-separated list of candidate groups            |
| Form Key        | Form identifier for custom rendering                |
| Due Date        | Task deadline expression                            |
| Priority        | Task priority (integer)                             |

#### Service Task Properties
| Property            | Description                                     |
|--------------------|-------------------------------------------------|
| Java Class         | Fully qualified class name for JavaDelegate     |
| Expression         | UEL expression (e.g., `${myService.execute()}`) |
| Delegate Expression| Expression resolving to a JavaDelegate          |
| Result Variable    | Variable to store the result                    |

#### Async Properties (all activities/gateways/events)
| Property    | Description                                |
|------------|---------------------------------------------|
| Async      | Enable async execution                      |
| Async Before | Execute before the element asynchronously |
| Async After  | Execute after the element asynchronously  |
| Exclusive    | Exclusive async execution (no parallel)   |

### Importing BPMN Files

You can import existing BPMN 2.0 XML files into the visual editor:

1. Click **Import** in the top-right toolbar
2. Select a `.bpmn` or `.bpmn20.xml` file from your computer
3. The diagram renders in the canvas and the process name auto-fills from the filename
4. Review and edit the process in the visual editor
5. Click **Deploy** to deploy it to the Flowable engine

Compatible sources for BPMN files include:
- [Flowable GitHub repo](https://github.com/flowable/flowable-engine) — official samples (Vacation Request, Helpdesk, Review Sales Lead, etc.)
- Any BPMN 2.0 compliant modeler (Camunda Modeler, Signavio, etc.)
- Exported files from this platform's **Export** button

> **Note:** Files using the legacy `activiti:` namespace are supported by Flowable but `flowable:` is recommended. The process must have `isExecutable="true"`.

Sample BPMN files are included in `e2e/samples/` for testing:
- `vacation-request.bpmn20.xml` — multi-step vacation approval with manager review, approval/rejection gateway, and resubmission loop
- `review-sales-lead.bpmn20.xml` — complex workflow with embedded subprocess, parallel gateway, error boundary events, and CRM integration task

### Exporting BPMN Files

Click **Export** to download the current diagram as a `.bpmn` XML file. This preserves all Flowable-specific properties (assignee, candidateGroups, formKey, async settings, etc.).

### Editing Existing Processes

From the Process Definitions list, click **Edit** on any process to load its BPMN XML into the visual editor. You can modify the diagram and redeploy — Flowable auto-increments the version number.

### Deploying from the Designer

1. Create or import your BPMN diagram in the editor
2. Enter a **process name** in the top-right input
3. Click **Deploy**
4. The process is sent to the workflow-service and deployed to the Flowable engine
5. You'll be redirected to the Process Definitions list

> **Important:** The BPMN process must have `isExecutable="true"` and a valid process `id` for deployment to succeed.

---

## Managing Process Definitions

Navigate to **Process Definitions** in the sidebar.

The table shows all deployed process definitions with:

| Column  | Description                          |
|---------|--------------------------------------|
| Name    | Process display name                 |
| Key     | Process definition key (unique ID)   |
| Version | Deployment version (auto-incremented)|
| Actions | Edit (open in designer), Delete      |

### Deleting a Process

Click **Delete** to remove a deployment. This removes the process definition but does not affect running instances.

> **Warning:** Deleting a process definition is irreversible. Running instances will continue to completion but the process cannot be started again.

---

## Custom Field Schemas

Navigate to **Custom Fields** in the sidebar to define dynamic form fields for processes.

Custom fields allow you to attach structured data to process instances without modifying the BPMN definition.

### Creating a Field Schema

1. Select a **process definition** from the dropdown
2. Fill in the field configuration:
   - **Field Key** — unique identifier (e.g., `customer_name`)
   - **Label** — display label (e.g., "Customer Name")
   - **Field Type** — one of: TEXT, TEXTAREA, NUMBER, DATE, DATETIME, BOOLEAN, DROPDOWN, MULTI_SELECT, FILE, USER_PICKER
   - **Required** — whether the field is mandatory
3. Click **Add**

### Managing Fields

The table shows all field schemas for the selected process:

| Column   | Description              |
|----------|--------------------------|
| Key      | Field identifier         |
| Label    | Display label            |
| Type     | Field data type          |
| Required | Yes/No                   |
| Actions  | Delete button            |

### How Custom Fields Work

- Schemas are stored in the custom-fields-service (separate from the workflow engine)
- Field values are associated with process instances
- The User Portal displays custom fields on the Task Detail page
- Fields are queried via the gateway: `GET /api/fields/values?processInstanceId=...`

---

## Audit Log

Navigate to **Audit Log** in the sidebar.

The audit log captures every significant event in the platform, recorded asynchronously via RabbitMQ.

### Viewing the Audit Log

The table displays audit entries with columns:

| Column    | Description                          |
|-----------|--------------------------------------|
| Timestamp | When the event occurred              |
| Event     | Event type (e.g., `task.completed`)  |
| Entity    | Entity type (PROCESS or TASK)        |
| Entity ID | ID of the affected entity            |
| User      | User who triggered the event         |

### Filtering

- **Type filter** — filter by entity type (All, Process, Task)
- **User ID** — filter by user who performed the action
- **Pagination** — navigate through pages (20 entries per page)

### Event Types

| Event Type         | Description                        |
|-------------------|------------------------------------|
| `process.started` | A new process instance was created |
| `process.completed`| A process instance finished       |
| `task.created`    | A new task was created             |
| `task.assigned`   | A task was assigned to a user      |
| `task.completed`  | A task was marked as complete      |

---

## Keycloak Administration

Keycloak manages authentication and tenant isolation.

**Admin Console:** `http://localhost:8180/admin`
**Realm:** `workflow-platform`

### Realm Structure

- **Realm:** `workflow-platform` — single realm for all tenants
- **Clients:**
  - `wfp-admin-portal` — OIDC client for admin portal
  - `wfp-user-portal` — OIDC client for user portal
- **Client Scopes:**
  - `tenant` — adds `tenant_id` claim to JWT
  - `profile` — adds `preferred_username`, `given_name`, `family_name`
  - `email` — adds `email` claim
  - `roles` — adds `realm_access.roles` claim

### Managing Users

1. Navigate to Keycloak admin console → Users
2. Click **Add User**
3. Set username, email, first/last name
4. Under **Credentials**, set a password
5. Under **Attributes**, add `tenant_id` attribute with the tenant value (e.g., `tenant-a`)

### Tenant Isolation

Tenant isolation is enforced through the `tenant_id` JWT claim:

1. Each user has a `tenant_id` attribute in Keycloak
2. The `tenant` client scope maps this attribute to the JWT `tenant_id` claim
3. The gateway extracts the claim and adds `X-Tenant-Id` header
4. Each service reads the header and filters all database queries by tenant

### Default Configuration

| User      | Tenant   | Attributes                        |
|-----------|----------|-----------------------------------|
| `admin-a` | tenant-a | `tenant_id=tenant-a`             |
| `admin-b` | tenant-b | `tenant_id=tenant-b`             |

---

## RabbitMQ Monitoring

**Management UI:** `http://localhost:15672`
**Credentials:** `wfp` / `wfp_secret`

### Key Resources

| Resource | Type     | Description                           |
|----------|----------|---------------------------------------|
| `wfp.events` | Exchange (topic) | All platform events published here |
| `wfp.notification` | Queue | Consumes task.* and process.completed events |
| `wfp.audit` | Queue | Consumes all events (#)              |

### Monitoring Checklist

1. **Overview** — verify connections from all 4 backend services
2. **Exchanges** — `wfp.events` should have bindings to both queues
3. **Queues** — both queues should show 0 messages (consumed in real-time)
4. **Connections** — 4 connections (one per service)

### Troubleshooting

- **Messages accumulating in queue:** Consumer service may be down. Check container logs.
- **No bindings on exchange:** Services haven't started yet. Wait for Spring Boot initialization.
- **Dead-lettered messages:** Check the service logs for deserialization errors. Ensure `Jackson2JsonMessageConverter` is configured.

---

## Docker Deployment

### Starting the Full Stack

```bash
# Build all images
docker compose -f docker/docker-compose.yml build

# Start all containers
docker compose -f docker/docker-compose.yml up -d

# Check container status
docker compose -f docker/docker-compose.yml ps
```

### Container Overview

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

### Startup Order

The docker-compose file defines dependencies:

1. **PostgreSQL** starts first (health check: `pg_isready`)
2. **RabbitMQ** starts first (health check: `rabbitmq-diagnostics -q ping`)
3. **Keycloak** starts after PostgreSQL is healthy
4. **Backend services** start after PostgreSQL and RabbitMQ are healthy
5. **Frontend containers** start after gateway is running

### Viewing Logs

```bash
# All services
docker compose -f docker/docker-compose.yml logs -f

# Single service
docker compose -f docker/docker-compose.yml logs -f workflow-service

# Last 100 lines
docker compose -f docker/docker-compose.yml logs --tail=100 workflow-service
```

### Database Schemas

PostgreSQL uses separate schemas per service (created by `docker/init-db.sql`):

| Schema          | Service              |
|----------------|----------------------|
| `workflow`     | workflow-service     |
| `custom_fields`| custom-fields-service|
| `notification` | notification-service |
| `audit`        | audit-service        |
| `keycloak`     | Keycloak             |

### Environment Variables

Backend service URIs are configured via environment variables in `docker-compose.yml`:

| Variable                | Default                | Description               |
|------------------------|------------------------|---------------------------|
| `WORKFLOW_SERVICE_URL` | `http://workflow:8081` | Workflow service URI      |
| `CUSTOM_FIELDS_SERVICE_URL` | `http://custom-fields:8082` | Custom fields URI |
| `NOTIFICATION_SERVICE_URL` | `http://notification:8083` | Notification URI    |
| `AUDIT_SERVICE_URL`    | `http://audit:8084`    | Audit service URI         |

---

## Kubernetes Deployment

The platform includes Helm charts for Kubernetes deployment.

### Chart Structure

```
helm/
├── charts/
│   ├── workflow-service/
│   ├── custom-fields-service/
│   ├── notification-service/
│   ├── audit-service/
│   ├── gateway/
│   ├── admin-portal/
│   └── user-portal/
└── workflow-platform/          # Umbrella chart
    ├── Chart.yaml
    └── values-local.yaml
```

### Deploying

```bash
# Update dependencies (pulls bitnami charts for PG, RabbitMQ)
helm dependency update helm/workflow-platform/

# Install
helm install wfp helm/workflow-platform/ \
  -f helm/workflow-platform/values-local.yaml

# Upgrade
helm upgrade wfp helm/workflow-platform/ \
  -f helm/workflow-platform/values-local.yaml
```

### Configuration

Override values in `values-local.yaml` or pass `--set` flags:

```yaml
# Example: override image tag
workflow-service:
  image:
    tag: "latest"

# Example: override database host
global:
  postgresql:
    host: my-pg-host.example.com
```

---

## Troubleshooting

### Common Issues

| Symptom | Cause | Fix |
|---------|-------|-----|
| Service fails to start with `DataSource` error | Missing database schema | Run `docker/init-db.sql` against PostgreSQL |
| `Failed to configure a DataSource` on gateway | Gateway doesn't need a DB | Verify gateway excludes `DataSourceAutoConfiguration` |
| Duplicate `@FilterDef` error | Two entities define `@FilterDef(name = "tenantFilter")` | Only ONE entity per persistence unit should have `@FilterDef`; others use `@Filter` only |
| RabbitMQ connection refused | RabbitMQ not ready | Wait for RabbitMQ health check to pass |
| JWT validation fails | Keycloak not reachable | Check Keycloak is running and `issuer-uri` is correct |
| Frontend shows "Loading..." | API calls failing | Check browser console for errors; verify gateway is running |
| Port 5432 conflict | Local PostgreSQL running | Docker maps PG to port 5433 to avoid conflicts |
| `gradlew` permission denied | File not executable | Run `git update-index --chmod=+x gradlew` |
| Flowable + H2 test failures | Wrong H2 mode | Use `MODE=LEGACY` in JDBC URL, not `MODE=PostgreSQL` |

### Health Checks

Verify all services are healthy:

```bash
# Gateway
curl http://localhost:9080/actuator/health

# Individual services
curl http://localhost:8081/actuator/health  # workflow
curl http://localhost:8082/actuator/health  # custom-fields
curl http://localhost:8083/actuator/health  # notification
curl http://localhost:8084/actuator/health  # audit
```

### Useful Commands

```bash
# Get a JWT token
curl -s -X POST 'http://localhost:8180/realms/workflow-platform/protocol/openid-connect/token' \
  -d 'grant_type=password&client_id=wfp-admin-portal&username=admin-a&password=password' \
  | jq .access_token -r

# Deploy a BPMN process
curl -X POST http://localhost:9080/api/workflow/deployments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"My Process","bpmnXml":"<xml>..."}'

# List tasks
curl http://localhost:9080/api/workflow/tasks?assignee=admin-a \
  -H "Authorization: Bearer $TOKEN"

# View audit log
curl http://localhost:9080/api/audit \
  -H "Authorization: Bearer $TOKEN"
```
