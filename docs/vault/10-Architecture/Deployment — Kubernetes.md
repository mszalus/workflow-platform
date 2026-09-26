---
title: Deployment — Kubernetes
tags:
  - architecture
  - deployment
type: architecture
source: docs/architecture/c4-deployment.md
---
[[Architecture MOC]] › [[Deployment Topologies]] › **Deployment — Kubernetes**

Umbrella [[Helm and Kubernetes|Helm]] chart with 10 sub-charts. Supports local (minikube) and GCP production profiles.

```mermaid
flowchart TD
    subgraph "Kubernetes Cluster"
        subgraph "Ingress"
            ING["Ingress Controller"]
        end

        subgraph "Application Pods"
            GW["Gateway<br/>2 replicas (GCP)"]
            WF["Workflow Service<br/>2-5 replicas (HPA)"]
            CF["Custom Fields<br/>2-3 replicas (HPA)"]
            NS["Notification<br/>2-3 replicas (HPA)"]
            AS["Audit<br/>2-3 replicas (HPA)"]
            AP["Admin Portal<br/>2 replicas"]
            UP["User Portal<br/>2 replicas"]
        end

        subgraph "Infrastructure (local only)"
            PG["PostgreSQL<br/>(Bitnami chart)"]
            RMQ["RabbitMQ<br/>(Bitnami chart)"]
            KC["Keycloak<br/>(Bitnami chart)"]
        end

        subgraph "GCP Managed Services"
            CSQL["Cloud SQL Proxy"]
        end
    end

    ING --> GW
    ING --> AP
    ING --> UP
    GW --> WF & CF & NS & AS
    WF & CF & NS & AS --> PG
    WF & NS & AS --> RMQ
    WF & CF & NS & AS -.-> CSQL

    style CSQL fill:#4285f4,stroke:#fff,color:#fff
```

## Helm Values Profiles

| Profile | File | Infra Charts | Replicas | Autoscaling |
|---------|------|-------------|----------|-------------|
| Local | `values-local.yaml` | Enabled (Bitnami PG, RMQ, KC) | 1 each | No |
| GCP | `values-gcp.yaml` | Disabled (managed services) | 2+ each | Yes (HPA) |

## Resource Allocation (GCP)

| Service | CPU Request | CPU Limit | Memory Request | Memory Limit | Min/Max Replicas |
|---------|------------|-----------|---------------|-------------|-----------------|
| Gateway | 250m | 500m | 256Mi | 512Mi | 2 / 5 |
| [[Workflow Service]] | 500m | 1000m | 512Mi | 1Gi | 2 / 5 |
| Custom Fields | 250m | 500m | 512Mi | 1Gi | 2 / 3 |
| Notification | 250m | 500m | 512Mi | 1Gi | 2 / 3 |
| Audit | 250m | 500m | 512Mi | 1Gi | 2 / 3 |
| [[Admin Portal]] | 50m | 200m | 64Mi | 128Mi | 2 / - |
| [[User Portal]] | 50m | 200m | 64Mi | 128Mi | 2 / - |


---

**Deployment topologies** — ← [[Deployment — GCP]]

> [!abstract]- All notes in this set
> [[Deployment — Docker Compose]]
> [[Deployment — GCP]]
