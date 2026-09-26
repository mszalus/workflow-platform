---
title: Terraform — What Was Built
tags:
  - ops
  - gcp
  - terraform
  - helm
type: report
source: terraform-helm-gcp-report.md
---
[[Operations MOC]] › **Terraform — What Was Built**

## Step 16a: Terraform Modules

Complete Terraform code for GCP infrastructure at `terraform/`. All modules are syntactically correct and wired together.

### Module overview

| Module | Purpose | Key resources |
|---|---|---|
| `networking` | VPC, subnets, Cloud NAT, VPC peering | google_compute_network, google_compute_subnetwork, google_compute_router, google_compute_router_nat, google_compute_global_address, google_service_networking_connection |
| `gke` | GKE Standard cluster | google_container_cluster (private nodes, Workload Identity, Managed [[Observability Stack|Prometheus]], REGULAR release channel), google_container_node_pool (autoscaling, Spot support, Shielded VMs) |
| `cloudsql` | PostgreSQL 16 | google_sql_database_instance (private IP, HA option, PITR, query insights), google_sql_user, google_sql_database |
| `artifact-registry` | Docker image storage | google_artifact_registry_repository (cleanup: keep 20 versions, delete untagged after 14d), google_artifact_registry_repository_iam_member |
| `iam` | Service accounts + Workload Identity | google_service_account (workload + cicd), WI IAM binding, project IAM roles (cloudtrace.agent, monitoring.metricWriter, logging.logWriter, secretmanager.secretAccessor) |
| `secrets` | GCP Secret Manager | google_secret_manager_secret + versions for DB password, [[Security and JWT|Keycloak]] admin password, [[Event System|RabbitMQ]] password. IAM access grants to workload SA |

### Environment configuration

Per-environment variable files at `terraform/environments/{dev,prod}/terraform.tfvars.example`:

```
# Dev: e2-standard-2, db-g1-small, no HA, no Spot
# Prod: e2-standard-4, db-custom-2-7680, HA + PITR, Spot nodes
```

Usage:
```bash
cd terraform
cp environments/dev/terraform.tfvars.example terraform.tfvars
# Edit with real values
terraform init -backend-config=environments/dev/backend.tf
terraform apply
```

### Key design decisions

- **Private cluster:** GKE nodes have no public IPs; access via Cloud NAT
- **Workload Identity:** Pods use GSA without key files (most secure approach)
- **Cloud SQL private IP:** No Cloud SQL Auth Proxy needed from GKE pods (VPC peering)
- **Managed Prometheus:** GMP enabled on the cluster; pods discovered via `prometheus.io/scrape` annotations
- **Spot nodes (prod):** Enabled via `node_spot = true` for ~70% cost savings on compute
- **HA SQL (prod only):** Regional PostgreSQL with PITR; dev uses Zonal to save cost

---

## Step 16b: Helm Chart GCP Updates

All 5 backend service charts updated with GCP-ready templates:

### New templates added to each backend chart

| Template | Purpose |
|---|---|
| `serviceaccount.yaml` | K8s ServiceAccount with Workload Identity annotation |
| `external-secret.yaml` | ExternalSecret syncing DB/RabbitMQ passwords from Secret Manager |
| `pdb.yaml` | PodDisruptionBudget (enabled via `values-gcp.yaml`) |

### Deployment template updates

- `rollingUpdate: {maxSurge: 1, maxUnavailable: 0}` — zero-downtime rolling updates
- `secretEnv` support — ref individual keys from K8s secrets as env vars
- `serviceAccountName` — injected when `serviceAccount.create: true`

### New sub-chart: `helm/charts/otel-collector`

Deploys `otel/opentelemetry-collector-contrib` with `googlecloud` exporter for Cloud Trace.

```yaml
# values-gcp.yaml excerpt:
otel-collector:
  enabled: true
  gcpProjectId: "REPLACE_WITH_GCP_PROJECT_ID"
```

### Umbrella chart updates

`helm/workflow-platform/Chart.yaml`: Added `otel-collector` dependency (condition: `otel-collector.enabled`).

`helm/workflow-platform/templates/cluster-secret-store.yaml`: Creates `ClusterSecretStore` resource for the External Secrets Operator pointing to GCP Secret Manager.

`helm/workflow-platform/values-gcp.yaml`: Complete GCP deployment values covering:
- `externalSecrets.*` — ESO ClusterSecretStore configuration
- Per-service `secretEnv` — maps K8s secret keys to env vars
- Per-service `SPRING_DATASOURCE_URL` — Cloud SQL private IP
- `GOOGLE_CLOUD_PROJECT` — enables Cloud Trace log correlation
- `ingress.*` — GCE Ingress class + cert-manager TLS annotations
- `podDisruptionBudget.*` — enabled for gateway + [[Workflow Service|workflow-service]]

---


---

**Terraform + Helm on GCP** — [[Terraform — Prerequisites for GCP Deployment]] →

> [!abstract]- All notes in this set
> [[Terraform — Prerequisites for GCP Deployment]]
> [[Terraform — Validation Performed]]
