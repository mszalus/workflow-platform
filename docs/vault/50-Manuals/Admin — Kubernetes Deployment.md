---
title: Admin — Kubernetes Deployment
tags:
  - manual
  - admin
type: manual
source: docs/admin-manual.md
---
[[Manuals MOC]] › [[Admin Portal]] › **Admin — Kubernetes Deployment**

The platform includes [[Helm and Kubernetes|Helm]] charts for Kubernetes deployment.

## Chart Structure

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

## Deploying

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

## Configuration

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


---

**Admin manual** — ← [[Admin — Docker Deployment]] · [[Admin — Troubleshooting]] →

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
> [[Admin — Docker Deployment]]
> [[Admin — Troubleshooting]]
