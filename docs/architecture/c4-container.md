# C4 Level 2 — Container Diagram

Shows all deployable units (containers) within the Workflow Platform and their interactions.

```mermaid
C4Container
    title Workflow Platform — Container Diagram

    Person(admin, "Admin User", "Designs workflows, manages fields, views audit")
    Person(endUser, "End User", "Starts processes, completes tasks")

    System_Ext(keycloak, "Keycloak 25", "OIDC identity provider<br/>Realm: workflow-platform")

    Container_Boundary(wfp, "Workflow Platform") {

        Container(adminPortal, "Admin Portal", "React 18, TypeScript, Vite, nginx", "BPMN process designer, custom field editor, deployment management, audit log viewer")
        Container(userPortal, "User Portal", "React 18, TypeScript, Vite, nginx", "Task inbox, start process, notifications, dynamic forms")

        Container(gateway, "API Gateway", "Spring Cloud Gateway MVC, Java 21", "JWT validation, path routing, tenant header propagation. Port 8080")

        Container(workflowSvc, "Workflow Service", "Spring Boot 3.3, Flowable 7.1, Java 21", "BPMN engine: deploy, start, complete tasks, comments, attachments, history. Port 8081")
        Container(fieldsSvc, "Custom Fields Service", "Spring Boot 3.3, Java 21", "Dynamic field schemas and values per process definition. Port 8082")
        Container(notifSvc, "Notification Service", "Spring Boot 3.3, Java 21", "Event-driven notifications, unread count, mark-read. Port 8083")
        Container(auditSvc, "Audit Service", "Spring Boot 3.3, Java 21", "Event-driven audit trail, queryable by entity/user/time. Port 8084")

        ContainerDb(postgres, "PostgreSQL 16", "5 schemas: workflow, custom_fields, notification, audit, keycloak", "Shared instance, one schema per service")
        ContainerQueue(rabbitmq, "RabbitMQ 3.13", "Topic exchange: wfp.events", "Async event bus between services")
    }

    Rel(admin, adminPortal, "Uses", "HTTPS")
    Rel(endUser, userPortal, "Uses", "HTTPS")

    Rel(adminPortal, gateway, "API calls", "/api/* via nginx proxy")
    Rel(userPortal, gateway, "API calls", "/api/* via nginx proxy")
    Rel(adminPortal, keycloak, "OAuth2 login", "OIDC Code Flow")
    Rel(userPortal, keycloak, "OAuth2 login", "OIDC Code Flow")

    Rel(gateway, workflowSvc, "Routes", "/api/workflow/** -> /api/**")
    Rel(gateway, fieldsSvc, "Routes", "/api/fields/** -> /api/**")
    Rel(gateway, notifSvc, "Routes", "/api/notifications/**")
    Rel(gateway, auditSvc, "Routes", "/api/audit/**")
    Rel(gateway, keycloak, "Validates JWTs", "JWK Set endpoint")

    Rel(workflowSvc, postgres, "Reads/Writes", "JDBC, schema: workflow")
    Rel(fieldsSvc, postgres, "Reads/Writes", "JDBC, schema: custom_fields")
    Rel(notifSvc, postgres, "Reads/Writes", "JDBC, schema: notification")
    Rel(auditSvc, postgres, "Reads/Writes", "JDBC, schema: audit")
    Rel(keycloak, postgres, "Reads/Writes", "JDBC, schema: keycloak")

    Rel(workflowSvc, rabbitmq, "Publishes events", "task.*, process.*")
    Rel(rabbitmq, notifSvc, "Delivers events", "queue: wfp.notification<br/>binds: task.*, process.completed")
    Rel(rabbitmq, auditSvc, "Delivers events", "queue: wfp.audit<br/>binds: # (all)")

    UpdateLayoutConfig($c4ShapeInRow="4", $c4BoundaryInRow="1")
```

## Container Inventory

| Container | Technology | Port | Database Schema | Description |
|-----------|-----------|------|----------------|-------------|
| Admin Portal | React 18 + nginx | 5173 (host) | - | Process designer, field editor, audit viewer |
| User Portal | React 18 + nginx | 5174 (host) | - | Task inbox, start process, notifications |
| API Gateway | Spring Cloud Gateway MVC | 9080 (host) / 8080 | - (no DB) | JWT validation, routing, tenant propagation |
| Workflow Service | Spring Boot + Flowable 7.1 | 8081 | `workflow` | BPMN engine, process/task lifecycle |
| Custom Fields Service | Spring Boot | 8082 | `custom_fields` | Dynamic field schemas & values |
| Notification Service | Spring Boot | 8083 | `notification` | Event-driven notifications |
| Audit Service | Spring Boot | 8084 | `audit` | Event-driven audit trail |
| PostgreSQL | PostgreSQL 16 | 5433 (host) / 5432 | all 5 schemas | Shared database instance |
| RabbitMQ | RabbitMQ 3.13 | 5672 / 15672 | - | Async event bus |
| Keycloak | Keycloak 25 | 8180 (host) / 8080 | `keycloak` | OIDC identity provider |

## Gateway Routing Rules

| External Path | Target Service | Rewrite Rule |
|---------------|---------------|-------------|
| `/api/workflow/**` | workflow-service:8081 | `RewritePath=/api/workflow(?:/(?<segment>.*))?$, /api/${segment}` |
| `/api/fields/**` | custom-fields-service:8082 | `RewritePath=/api/fields(?:/(?<segment>.*))?$, /api/${segment}` |
| `/api/notifications/**` | notification-service:8083 | Pass-through (no rewrite) |
| `/api/audit/**` | audit-service:8084 | Pass-through (no rewrite) |

## Event Flows (RabbitMQ)

| Producer | Routing Key | Consumer(s) | Purpose |
|----------|------------|-------------|---------|
| Workflow Service | `process.started` | Audit Service | Audit trail |
| Workflow Service | `process.completed` | Notification Service, Audit Service | User notification + audit |
| Workflow Service | `process.cancelled` | Audit Service | Audit trail |
| Workflow Service | `task.created` | Notification Service, Audit Service | New task notification + audit |
| Workflow Service | `task.assigned` | Notification Service, Audit Service | Assignment notification + audit |
| Workflow Service | `task.completed` | Notification Service, Audit Service | Completion notification + audit |
| Workflow Service | `task.delegated` | Notification Service, Audit Service | Delegation notification + audit |

## Notes for Editors

- **Adding a new service**: Add a `Container` node, a `Rel` to `postgres` (with its schema name), a `Rel` from `gateway`, and update the gateway routing table. If it consumes events, add a `Rel` from `rabbitmq`.
- **Adding direct inter-service calls**: Currently services communicate only via RabbitMQ (async). If you add synchronous service-to-service HTTP calls, add `Rel` edges between the service containers and note the coupling trade-off.
- **Splitting the database**: If a service needs its own PostgreSQL instance, replace the single `ContainerDb` with multiple and update the `Rel` edges accordingly.
