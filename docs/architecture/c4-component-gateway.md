# C4 Level 3 — Component Diagram: API Gateway

The gateway is a thin routing + security layer with no database. It validates JWTs, extracts the tenant ID, and routes requests to backend services.

```mermaid
C4Component
    title API Gateway — Component Diagram

    Container_Ext(adminPortal, "Admin Portal", "nginx reverse proxy")
    Container_Ext(userPortal, "User Portal", "nginx reverse proxy")
    Container_Ext(keycloak, "Keycloak", "JWK Set endpoint")

    Container_Ext(workflowSvc, "Workflow Service", "Port 8081")
    Container_Ext(fieldsSvc, "Custom Fields Service", "Port 8082")
    Container_Ext(notifSvc, "Notification Service", "Port 8083")
    Container_Ext(auditSvc, "Audit Service", "Port 8084")

    Container_Boundary(gateway, "API Gateway") {

        Component(scgRoutes, "Route Definitions", "Spring Cloud Gateway MVC", "4 route rules mapping external paths to backend services. Uses RewritePath for workflow and fields routes.")

        Component(jwtFilter, "JWT Validation", "Spring Security OAuth2 Resource Server", "Validates JWT signature against Keycloak JWK Set. Extracts claims (preferred_username, tenant_id, roles).")

        Component(tenantFilter, "TenantHeaderFilter", "Gateway Filter", "Reads tenant_id from JWT claims. Adds X-Tenant-Id header to downstream requests.")

        Component(corsConfig, "CORS Configuration", "Spring Security", "Allows configured origins for cross-origin requests from frontend portals.")

        Component(healthEndpoint, "Actuator Health", "Spring Boot Actuator", "Public endpoint: /actuator/health. No JWT required.")
    }

    Rel(adminPortal, scgRoutes, "/api/* requests")
    Rel(userPortal, scgRoutes, "/api/* requests")

    Rel(scgRoutes, jwtFilter, "All /api/** requests")
    Rel(jwtFilter, keycloak, "Fetches JWK Set", "/.well-known/openid-configuration")
    Rel(jwtFilter, tenantFilter, "Authenticated request")

    Rel(tenantFilter, workflowSvc, "/api/workflow/**", "RewritePath -> /api/**")
    Rel(tenantFilter, fieldsSvc, "/api/fields/**", "RewritePath -> /api/**")
    Rel(tenantFilter, notifSvc, "/api/notifications/**", "Pass-through")
    Rel(tenantFilter, auditSvc, "/api/audit/**", "Pass-through")

    UpdateLayoutConfig($c4ShapeInRow="4", $c4BoundaryInRow="1")
```

## Request Flow

```
Browser -> nginx (frontend) -> Gateway:9080
  1. Route matcher selects backend based on path prefix
  2. JWT filter validates token signature via Keycloak JWK Set
  3. TenantHeaderFilter extracts tenant_id claim -> X-Tenant-Id header
  4. RewritePath (if applicable) transforms the URL
  5. Request forwarded to backend service with Authorization + X-Tenant-Id headers
```

## Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| No database | Gateway is stateless — all state lives in JWTs and downstream services |
| Excludes `DataSourceAutoConfiguration` | `wfp-security` lib pulls in JPA transitively; gateway must opt out |
| Named env vars for service URLs | Avoids partial route override bugs from indexed Spring Gateway env vars |
| No RewritePath for notification/audit | These services expose paths that already match the gateway's external paths |

## Notes for Editors

- **Adding a new backend route**: Add a route entry in `application.yml` under `spring.cloud.gateway.mvc.routes`. Decide whether the service needs `RewritePath` or can use pass-through.
- **Adding rate limiting or circuit breaking**: Add Spring Cloud Gateway filters to the route definitions.
- **Switching to service discovery**: Replace hard-coded `uri` values with `lb://service-name` and add a discovery client (Eureka, Consul, or Kubernetes).
