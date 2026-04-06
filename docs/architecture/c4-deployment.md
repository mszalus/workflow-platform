# C4 Level 4 — Deployment Diagrams

Two deployment topologies: Docker Compose (local dev) and GCP (production-like).

## Docker Compose Deployment

Local development stack — 10 containers on a single Docker host.

```mermaid
architecture-beta
    group dockerHost(cloud)[Docker Host]

    group infra(server)[Infrastructure] in dockerHost
        service postgres(database)[PostgreSQL 16] in infra
        service rabbitmq(server)[RabbitMQ 3.13] in infra
        service keycloak(server)[Keycloak 25] in infra

    group backends(server)[Backend Services] in dockerHost
        service gateway(server)[Gateway :9080] in backends
        service workflow(server)[Workflow :8081] in backends
        service fields(server)[Custom Fields :8082] in backends
        service notif(server)[Notification :8083] in backends
        service audit(server)[Audit :8084] in backends

    group frontends(server)[Frontends] in dockerHost
        service adminPortal(server)[Admin Portal :5173] in frontends
        service userPortal(server)[User Portal :5174] in frontends
```

### Docker Compose — Port Mapping & Dependencies

| Container | Image | Host Port | Container Port | Depends On |
|-----------|-------|-----------|---------------|------------|
| `wfp-postgres` | postgres:16-alpine | 5433 | 5432 | - |
| `wfp-rabbitmq` | rabbitmq:3.13-management-alpine | 5672, 15672 | 5672, 15672 | - |
| `wfp-keycloak` | quay.io/keycloak/keycloak:25.0.6 | 8180 | 8080 | postgres (healthy) |
| `wfp-gateway` | wfp/gateway | 9080 | 8080 | keycloak (started) |
| `wfp-workflow` | wfp/workflow-service | - | 8081 | postgres (healthy), rabbitmq (healthy) |
| `wfp-custom-fields` | wfp/custom-fields-service | - | 8082 | postgres (healthy) |
| `wfp-notification` | wfp/notification-service | - | 8083 | postgres (healthy), rabbitmq (healthy) |
| `wfp-audit` | wfp/audit-service | - | 8084 | postgres (healthy), rabbitmq (healthy) |
| `wfp-admin-portal` | wfp/admin-portal | 5173 | 80 | gateway (started) |
| `wfp-user-portal` | wfp/user-portal | 5174 | 80 | gateway (started) |

### Startup Order

```mermaid
flowchart LR
    PG[PostgreSQL] --> KC[Keycloak]
    PG --> WF[Workflow Service]
    PG --> CF[Custom Fields Service]
    PG --> NS[Notification Service]
    PG --> AS[Audit Service]
    RMQ[RabbitMQ] --> WF
    RMQ --> NS
    RMQ --> AS
    KC --> GW[Gateway]
    GW --> AP[Admin Portal]
    GW --> UP[User Portal]
```

### Database Schemas

Single PostgreSQL instance with 5 schemas:

```mermaid
flowchart TD
    subgraph "PostgreSQL (wfp database)"
        KS["keycloak schema<br/>Keycloak managed tables"]
        WS["workflow schema<br/>wf_process_metadata, wf_comments, wf_attachments<br/>+ Flowable ACT_* tables"]
        CFS["custom_fields schema<br/>field_schema, field_option, field_value"]
        NS["notification schema<br/>notification, notification_preference"]
        AS["audit schema<br/>audit_entry"]
    end

    KC[Keycloak] --> KS
    WF[Workflow Service] --> WS
    CF[Custom Fields Service] --> CFS
    NF[Notification Service] --> NS
    AU[Audit Service] --> AS
```

### Network Topology (Docker)

```mermaid
flowchart TD
    Browser["Browser"]

    Browser -->|":5173"| AP["Admin Portal<br/>(nginx)"]
    Browser -->|":5174"| UP["User Portal<br/>(nginx)"]
    Browser -->|":8180"| KC["Keycloak"]

    AP -->|"/api/* proxy"| GW["Gateway :9080"]
    UP -->|"/api/* proxy"| GW

    GW -->|"/api/workflow/**"| WF["Workflow Service :8081"]
    GW -->|"/api/fields/**"| CF["Custom Fields :8082"]
    GW -->|"/api/notifications/**"| NS["Notification :8083"]
    GW -->|"/api/audit/**"| AS["Audit :8084"]

    GW -.->|"JWK Set"| KC

    WF --> PG["PostgreSQL :5432"]
    CF --> PG
    NS --> PG
    AS --> PG
    KC --> PG

    WF -->|"publish"| RMQ["RabbitMQ :5672"]
    RMQ -->|"wfp.notification"| NS
    RMQ -->|"wfp.audit"| AS

    style Browser fill:#f9f,stroke:#333
    style PG fill:#336,stroke:#fff,color:#fff
    style RMQ fill:#f60,stroke:#fff,color:#fff
    style KC fill:#6a3,stroke:#fff,color:#fff
```

---

## GCP Deployment

Single Compute Engine VM running the same Docker Compose stack.

```mermaid
flowchart TD
    subgraph "GCP Project"
        subgraph "Artifact Registry"
            AR["us-central1-docker.pkg.dev<br/>/PROJECT_ID/wfp-images<br/>7 Docker images"]
        end

        subgraph "Compute Engine"
            subgraph "e2-medium VM (2 vCPU, 4GB)"
                DC["Docker Compose<br/>(same 10 containers as local)"]
            end
        end

        FW["Firewall Rule: allow-wfp<br/>Ports: 5173, 5174, 8180, 9080"]
        CS["Cloud Scheduler<br/>Auto-stop VM at midnight UTC"]
    end

    Internet["Internet"] -->|"Allowed ports"| FW
    FW --> DC
    CS -.->|"Stop VM"| DC
    AR -.->|"Pull images"| DC
```

### GCP Configuration

| Resource | Spec | Notes |
|----------|------|-------|
| VM Instance | `e2-medium` (2 vCPU, 4GB) | Min viable for 10 containers; e2-small causes OOM |
| Disk | 30 GB `pd-standard` | OS + Docker images + data |
| Region | `us-central1-a` | - |
| Firewall | Ports 5173, 5174, 8180, 9080 | Tag: `wfp-server` |
| Auto-stop | Cloud Scheduler at midnight UTC | Cost savings (~$1/month when stopped) |
| Running cost | ~$25/month | e2-medium on-demand |

### GCP-Specific Overrides

| Config | Local Value | GCP Value |
|--------|------------|-----------|
| JWT Issuer URI | `http://localhost:8180/...` | `http://{EXTERNAL_IP}:8180/...` |
| Keycloak `KC_HOSTNAME` | `localhost` | `{EXTERNAL_IP}` |
| Frontend `VITE_KEYCLOAK_URL` | `http://localhost:8180` | `http://{EXTERNAL_IP}:8180` |

---

## Kubernetes Deployment (Helm)

Umbrella Helm chart with 10 sub-charts. Supports local (minikube) and GCP production profiles.

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

### Helm Values Profiles

| Profile | File | Infra Charts | Replicas | Autoscaling |
|---------|------|-------------|----------|-------------|
| Local | `values-local.yaml` | Enabled (Bitnami PG, RMQ, KC) | 1 each | No |
| GCP | `values-gcp.yaml` | Disabled (managed services) | 2+ each | Yes (HPA) |

### Resource Allocation (GCP)

| Service | CPU Request | CPU Limit | Memory Request | Memory Limit | Min/Max Replicas |
|---------|------------|-----------|---------------|-------------|-----------------|
| Gateway | 250m | 500m | 256Mi | 512Mi | 2 / 5 |
| Workflow Service | 500m | 1000m | 512Mi | 1Gi | 2 / 5 |
| Custom Fields | 250m | 500m | 512Mi | 1Gi | 2 / 3 |
| Notification | 250m | 500m | 512Mi | 1Gi | 2 / 3 |
| Audit | 250m | 500m | 512Mi | 1Gi | 2 / 3 |
| Admin Portal | 50m | 200m | 64Mi | 128Mi | 2 / - |
| User Portal | 50m | 200m | 64Mi | 128Mi | 2 / - |

## Notes for Editors

- **Adding a new service to Docker Compose**: Add the service definition to `docker/docker-compose.yml`, create its Dockerfile, and add the schema to `docker/init-db.sql` if it needs a database.
- **Adding a new service to Helm**: Create a sub-chart under `helm/charts/`, add it as a dependency in `helm/workflow-platform/Chart.yaml`, and add its configuration to both `values-local.yaml` and `values-gcp.yaml`.
- **Switching GCP from VM to GKE**: Replace the Compute Engine section with a GKE cluster, use the Helm chart for deployment, and switch to Cloud SQL (via proxy) + Cloud Memorystore for RabbitMQ.
- **Adding a CDN or Load Balancer**: Insert it before the frontend containers in the network topology diagram.
