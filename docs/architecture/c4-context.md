# C4 Level 1 — System Context Diagram

Shows the Workflow Platform as a single system and its relationships with users and external systems.

```mermaid
C4Context
    title Workflow Platform — System Context

    Person(admin, "Admin User", "Designs BPMN workflows, manages custom fields, reviews audit logs")
    Person(endUser, "End User", "Starts processes, completes tasks, receives notifications")

    System(wfp, "Workflow Platform", "Multi-tenant BPMN workflow platform. Users design workflows visually, deploy them, and end users complete tasks through a task inbox. Every action is audited.")

    System_Ext(keycloak, "Keycloak", "OpenID Connect identity provider. Single realm with Organizations for tenant isolation. Issues JWTs with tenant_id claim.")

    System_Ext(browser, "Web Browser", "Hosts the React SPA frontends (Admin Portal, User Portal)")

    Rel(admin, browser, "Uses")
    Rel(endUser, browser, "Uses")
    Rel(browser, wfp, "HTTPS/REST (JWT Bearer)", "JSON over HTTPS")
    Rel(wfp, keycloak, "Validates JWTs via JWK Set", "OIDC/.well-known")
    Rel(browser, keycloak, "OAuth2 Authorization Code Flow", "OIDC login/token")

    UpdateLayoutConfig($c4ShapeInRow="3", $c4BoundaryInRow="1")
```

## Actors

| Actor | Role | Interactions |
|-------|------|-------------|
| **Admin User** | Process designer, platform administrator | Deploy BPMN processes, configure custom field schemas, view audit trail |
| **End User** | Day-to-day workflow participant | Start processes, claim/complete tasks, view notifications |

## External Systems

| System | Purpose | Protocol |
|--------|---------|----------|
| **Keycloak** | Identity & access management, multi-tenant isolation via Organizations | OIDC, JWT, JWK Set |
| **Web Browser** | Hosts React SPAs that communicate with the platform API | HTTPS |

## Notes for Editors

- If you add a new external integration (e.g., email provider, external API), add it as a `System_Ext` node and a `Rel` edge.
- If you split the platform into separately deployable products, promote them from one `System` box to multiple `System` boxes at this level.
