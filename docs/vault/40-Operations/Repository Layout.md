---
title: Repository Layout
tags:
  - ops
type: reference
source: README.md
---
[[Operations MOC]] › **Repository Layout**

```
workflow-platform/
├── buildSrc/                    # Gradle convention plugins (Java 21, Spring Boot, Lombok)
├── libs/                        # Shared libraries
│   ├── wfp-common/              # DTOs, exception handling
│   ├── wfp-events/              # RabbitMQ event types (polymorphic Jackson)
│   ├── wfp-security/            # JWT auth, tenant context, Hibernate tenant filter
│   └── wfp-test-support/        # Test helpers (JWT mocking, Testcontainers)
├── services/
│   ├── gateway/                 # API Gateway (routing, JWT validation, tenant headers)
│   ├── workflow-service/        # Flowable BPMN engine + REST API
│   ├── custom-fields-service/   # Dynamic field schemas and values
│   ├── notification-service/    # Event-driven notifications
│   └── audit-service/           # Event-driven audit trail
├── frontend/
│   ├── packages/shared-ui/      # Shared components, API client, auth
│   ├── packages/bpmn-editor/    # bpmn-js wrapper
│   ├── apps/admin-portal/       # Process design + admin UI
│   └── apps/user-portal/        # Task inbox + user UI
├── docker/                      # Docker Compose + Keycloak realm config
└── helm/                        # Kubernetes Helm charts
```


---

**Running and shipping** — ← [[Tech Stack]] · [[Build Commands]] →

> [!abstract]- All notes in this set
> [[Prerequisites]]
> [[Quick Start]]
> [[Local Development]]
> [[Tech Stack]]
> [[Build Commands]]
> [[Docker Compose Stack]]
> [[CI Pipeline]]
> [[Helm and Kubernetes]]
> [[Observability Stack]]
> [[Ports and Endpoints]]
