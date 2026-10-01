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

        Container(gateway, "API Gateway", "Spring Cloud Gateway MVC, Java 21", "JWT validation, path routing. Port 8080")

        Container(workflowSvc, "Workflow Service", "Spring Boot 3.3, Flowable 7.1, Java 21", "BPMN engine: deploy, start, complete tasks, comments, attachments, history; custom field schemas and values; in-app notifications; audit trail. Port 8081")

        ContainerDb(postgres, "PostgreSQL 16", "2 schemas: workflow, keycloak", "Shared instance, one schema per service")
    }

    Rel(admin, adminPortal, "Uses", "HTTPS")
    Rel(endUser, userPortal, "Uses", "HTTPS")

    Rel(adminPortal, gateway, "API calls", "/api/* via nginx proxy")
    Rel(userPortal, gateway, "API calls", "/api/* via nginx proxy")
    Rel(adminPortal, keycloak, "OAuth2 login", "OIDC Code Flow")
    Rel(userPortal, keycloak, "OAuth2 login", "OIDC Code Flow")

    Rel(gateway, workflowSvc, "Routes", "/api/workflow/** -> /api/**")
    Rel(gateway, workflowSvc, "Routes", "/api/fields/** -> /api/**")
    Rel(gateway, workflowSvc, "Routes", "/api/notifications/**")
    Rel(gateway, workflowSvc, "Routes", "/api/audit/**")
    Rel(gateway, keycloak, "Validates JWTs", "JWK Set endpoint")

    Rel(workflowSvc, postgres, "Reads/Writes", "JDBC, schema: workflow")
    Rel(keycloak, postgres, "Reads/Writes", "JDBC, schema: keycloak")


    UpdateLayoutConfig($c4ShapeInRow="4", $c4BoundaryInRow="1")
```

## Container Inventory

| Container | Technology | Port | Database Schema | Description |
|-----------|-----------|------|----------------|-------------|
| Admin Portal | React 18 + nginx | 5173 (host) | - | Process designer, field editor, audit viewer |
| User Portal | React 18 + nginx | 5174 (host) | - | Task inbox, start process, notifications |
| API Gateway | Spring Cloud Gateway MVC | 9080 (host) / 8080 | - (no DB) | JWT validation, routing |
| Workflow Service | Spring Boot + Flowable 7.1 | 8081 | `workflow` | BPMN engine, process/task lifecycle, custom fields, notifications, audit |
| PostgreSQL | PostgreSQL 16 | 5433 (host) / 5432 | all 2 schemas | Shared database instance |
| Keycloak | Keycloak 25 | 8180 (host) / 8080 | `keycloak` | OIDC identity provider |

## Gateway Routing Rules

| External Path | Target Service | Rewrite Rule |
|---------------|---------------|-------------|
| `/api/workflow/**` | workflow-service:8081 | `RewritePath=/api/workflow(?:/(?<segment>.*))?$, /api/${segment}` |
| `/api/fields/**` | workflow-service:8081 | `RewritePath=/api/fields(?:/(?<segment>.*))?$, /api/${segment}` |
| `/api/notifications/**` | workflow-service:8081 | Pass-through (no rewrite) |
| `/api/audit/**` | workflow-service:8081 | Pass-through (no rewrite) |

## Events

Workflow Service is the only producer and consumer. `EventPublisher` hands each event (`process.started`, `task.created`, `task.assigned`, `task.completed`, `task.delegated`) to `NotificationService` and `AuditService`, which write their rows in the same transaction as the change. There is no message broker.

## Notes for Editors

- **Adding a new service**: Add a `Container` node, a `Rel` to `postgres` (with its schema name), a `Rel` from `gateway`, and update the gateway routing table. 
- **Splitting the database**: If a service needs its own PostgreSQL instance, replace the single `ContainerDb` with multiple and update the `Rel` edges accordingly.
