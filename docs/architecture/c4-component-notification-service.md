# C4 Level 3 — Component Diagram: Notification Service

The notification service is entirely event-driven. It has no synchronous producers — all notifications are created by consuming RabbitMQ events from the workflow service.

```mermaid
C4Component
    title Notification Service — Component Diagram

    Container_Ext(gateway, "API Gateway", "Routes /api/notifications/** to this service")
    ContainerDb_Ext(postgres, "PostgreSQL", "Schema: notification")
    ContainerQueue_Ext(rabbitmq, "RabbitMQ", "Queue: wfp.notification")

    Container_Boundary(notifSvc, "Notification Service") {

        Component(notifCtrl, "NotificationController", "REST Controller", "GET /api/notifications (list, paginated)<br/>GET /api/notifications/unread-count<br/>PUT /api/notifications/mark-read<br/>PUT /api/notifications/mark-all-read")

        Component(notifService, "NotificationService", "Service", "Queries notifications by user+tenant, mark read, count unread, create notifications")

        Component(eventListener, "WorkflowEventListener", "RabbitMQ Listener", "Consumes task.* and process.completed events. Creates Notification entities. Handles null userId gracefully.")

        Component(tenantAspect, "TenantFilterAspect", "AOP Aspect", "Auto-enables Hibernate tenantFilter on REST requests")
        Component(securityConfig, "SecurityConfig", "Spring Security", "OAuth2 resource server, JWT validation")

        Component(notifRepo, "NotificationRepository", "JPA Repository", "CRUD for Notification entity. Custom queries: findByUserIdOrderByCreatedAtDesc, countByUserIdAndReadFalse, markAllReadByUserId")
        Component(prefRepo, "NotificationPreferenceRepository", "JPA Repository", "CRUD for NotificationPreference entity")
    }

    Rel(gateway, notifCtrl, "HTTP/JSON")
    Rel(notifCtrl, notifService, "Calls")
    Rel(notifService, notifRepo, "JPA")
    Rel(notifService, prefRepo, "JPA")
    Rel(notifRepo, postgres, "JDBC")
    Rel(prefRepo, postgres, "JDBC")

    Rel(rabbitmq, eventListener, "AMQP", "task.created, task.assigned,<br/>task.completed, process.completed")
    Rel(eventListener, notifService, "Creates notifications")

    UpdateLayoutConfig($c4ShapeInRow="3", $c4BoundaryInRow="1")
```

## Event-to-Notification Mapping

| Incoming Event | Notification Type | Target User | Reference |
|---------------|-------------------|-------------|-----------|
| `TaskCreatedEvent` | `TASK_ASSIGNED` | Task assignee | taskId |
| `TaskAssignedEvent` | `TASK_ASSIGNED` | Task assignee | taskId |
| `TaskCompletedEvent` | `TASK_COMPLETED` | User who completed | taskId |
| `ProcessCompletedEvent` | `PROCESS_COMPLETED` | Process initiator | processInstanceId |

## Notes for Editors

- **Adding a notification channel** (e.g., email, WebSocket push): Add a new component (e.g., `EmailSender`) called by `NotificationService.createNotification()`. Gate it with `NotificationPreference.emailEnabled`.
- **Adding new event types**: Update `WorkflowEventListener` with a new handler method. The queue binding (`task.*`, `process.completed`) may need expanding if the new event uses a different routing key prefix.
- **NotificationPreference** is defined but not yet enforced in the event listener — the preference check is a planned enhancement.
