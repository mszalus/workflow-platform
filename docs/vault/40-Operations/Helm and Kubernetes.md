---
title: Helm and Kubernetes
tags:
  - ops
type: reference
source: README.md
---
[[Operations MOC]] › **Helm and Kubernetes**

```bash
# Update Helm dependencies (pulls Bitnami charts for PG, RabbitMQ, Keycloak)
helm dependency update helm/workflow-platform/

# Install with local values
helm install wfp helm/workflow-platform/ -f helm/workflow-platform/values-local.yaml

# Lint charts
for chart in helm/charts/*/; do helm lint "$chart"; done
```


---

**Running and shipping** — ← [[CI Pipeline]] · [[Observability Stack]] →

> [!abstract]- All notes in this set
> [[Prerequisites]]
> [[Quick Start]]
> [[Local Development]]
> [[Tech Stack]]
> [[Repository Layout]]
> [[Build Commands]]
> [[Docker Compose Stack]]
> [[CI Pipeline]]
> [[Observability Stack]]
> [[Ports and Endpoints]]
