# Architecture Documentation

C4 model diagrams and data model for the Workflow Platform. All diagrams use [Mermaid](https://mermaid.js.org/) syntax — editable as plain text, rendered by GitHub and most Markdown viewers.

## Diagram Index

| Diagram | Level | File | Description |
|---------|-------|------|-------------|
| **System Context** | C4 L1 | [c4-context.md](c4-context.md) | Users, external systems, platform boundary |
| **Container** | C4 L2 | [c4-container.md](c4-container.md) | All services, databases, message broker, frontends, gateway |
| **Component: Workflow Service** | C4 L3 | [c4-component-workflow-service.md](c4-component-workflow-service.md) | Controllers, services, Flowable engine, event publisher |
| **Component: Notification Service** | C4 L3 | [c4-component-notification-service.md](c4-component-notification-service.md) | Event listener, notification CRUD, preference repository |
| **Component: API Gateway** | C4 L3 | [c4-component-gateway.md](c4-component-gateway.md) | JWT validation, tenant propagation, route definitions |
| **Deployment** | C4 L4 | [c4-deployment.md](c4-deployment.md) | Docker Compose, GCP VM, Kubernetes/Helm topologies |
| **Data Model** | ERD | [data-model.md](data-model.md) | All JPA entities, Flowable tables, cross-schema references |

## How to Use These Diagrams

### Viewing
- **GitHub**: Mermaid diagrams render automatically in `.md` files.
- **VS Code**: Install the [Mermaid Preview](https://marketplace.visualstudio.com/items?itemName=bierner.markdown-mermaid) extension.
- **CLI**: Use `mmdc` from `@mermaid-js/mermaid-cli` to export to PNG/SVG.

### Editing
1. Edit the Mermaid code blocks directly in the `.md` files.
2. Each file includes a **"Notes for Editors"** section explaining how to extend that diagram for common changes (adding services, entities, routes, etc.).
3. After editing, these diagrams serve as the source of truth for implementation — Claude Code reads them to understand the target architecture before making code changes.

### Conventions
- C4 diagrams use the `C4Context`, `C4Container`, `C4Component` Mermaid directives where supported, falling back to `flowchart` for complex layouts.
- Tables accompany each diagram with structured details that don't fit well in visual form.
- Cross-references between diagrams use relative links.
