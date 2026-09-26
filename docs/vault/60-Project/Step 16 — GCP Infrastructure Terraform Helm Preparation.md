---
title: Step 16 — GCP Infrastructure Terraform Helm Preparation
tags:
  - project
  - plan
  - status/partial
type: plan
source: PLAN.md
status: partial
---
[[Project MOC]] › **Step 16 — GCP Infrastructure Terraform Helm Preparation**

**Goal:** Create all GCP infrastructure as code so the platform can be deployed to GCP with a single `terraform apply` + `helm install`. No deployment happens in this step — only code is written and reviewed.

## 16.1 GCP APIs to enable (one-time, per project)

```bash
gcloud services enable \
  container.googleapis.com \
  sqladmin.googleapis.com \
  secretmanager.googleapis.com \
  artifactregistry.googleapis.com \
  cloudtrace.googleapis.com \
  monitoring.googleapis.com \
  logging.googleapis.com \
  dns.googleapis.com \
  certificatemanager.googleapis.com
```

## 16.2 Terraform structure to create

```
terraform/
├── modules/
│   ├── gke/           # GKE cluster + node pools
│   ├── cloudsql/      # PostgreSQL 16 instance + databases + users
│   ├── artifact-registry/   # Docker image repository
│   ├── secrets/       # Secret Manager entries (DB password, RabbitMQ creds, Keycloak admin)
│   ├── iam/           # Service accounts + Workload Identity bindings
│   └── networking/    # VPC, subnets, Cloud NAT, firewall rules
├── environments/
│   ├── dev/           # dev tfvars + state backend config
│   └── prod/          # prod tfvars + state backend config
├── main.tf
├── variables.tf
└── outputs.tf         # Outputs: cluster name, SQL connection name, registry URL
```

**Key Terraform resources:**

| Resource | Type | Notes |
|---|---|---|
| GKE cluster | `google_container_cluster` | Autopilot for dev; Standard n2-standard-4 for prod |
| Cloud SQL | `google_sql_database_instance` | PostgreSQL 16, private IP via VPC peering |
| Artifact Registry | `google_artifact_registry_repository` | Docker format, region-specific |
| Secret Manager | `google_secret_manager_secret` | DB password, [[Event System|RabbitMQ]] password, [[Security and JWT|Keycloak]] admin, JWT secret |
| Workload Identity | `google_service_account` + `google_iam_binding` | Pods write to Cloud Trace + Cloud Logging without key files |
| VPC | `google_compute_network` | Private cluster, no public node IPs |
| Cloud NAT | `google_compute_router_nat` | Outbound internet for pods (pull images, reach Keycloak) |
| Managed cert | `google_compute_managed_ssl_certificate` | TLS for the gateway ingress |

## 16.3 Helm changes required before GCP deployment

**a) External Secrets Operator (ESO)**

Install ESO to bridge Secret Manager → K8s Secrets. Add `ExternalSecret` CRDs for:
- `wfp-db-credentials` (DB username/password)
- `wfp-rabbitmq-credentials`
- `wfp-keycloak-admin`

Services reference these as `envFrom.secretRef` instead of plaintext env vars.

**b) cert-manager + Ingress**

Install `cert-manager` (via [[Helm and Kubernetes|Helm]]) with `ClusterIssuer` pointing to Let's Encrypt (or Google CA). Update gateway Helm chart to add:
```yaml
ingress:
  enabled: true
  className: gce           # GKE Ingress controller
  annotations:
    kubernetes.io/ingress.global-static-ip-name: wfp-gateway-ip
    networking.gke.io/managed-certificates: wfp-tls-cert
  hosts:
    - host: api.yourdomain.com
      paths: [{path: /, pathType: Prefix}]
```

**c) OTEL Collector for Cloud Trace**

Add a `wfp-otel-collector` Deployment (or DaemonSet) to the Helm umbrella chart with:
```yaml
config:
  exporters:
    googlecloud:
      project: ${GOOGLE_CLOUD_PROJECT}
  service:
    pipelines:
      traces:
        exporters: [googlecloud]
```
All backend service pods set `OTEL_EXPORTER_OTLP_ENDPOINT: http://wfp-otel-collector:4318`.

**d) Keycloak production mode**

The current docker-compose uses `start-dev`. For GCP, Keycloak must use `start --optimized` with a pre-built image. Add a `keycloak` sub-chart to the Helm umbrella (or use Bitnami Keycloak chart) with:
- `KC_DB`: Cloud SQL PostgreSQL via Cloud SQL Auth Proxy sidecar
- `KC_HOSTNAME`: the public Keycloak hostname (e.g., `auth.yourdomain.com`)
- Realm import via init container or import job

**e) RabbitMQ with persistence**

Replace `tmpfs` with persistent volumes. Use Bitnami RabbitMQ Helm chart with quorum queues enabled. Or consider GCP-managed alternatives (Google Cloud Pub/Sub with a bridge if scale demands it — this is a larger architectural change).

**f) Values files**

Update `helm/workflow-platform/values-gcp.yaml` with:
- `GOOGLE_CLOUD_PROJECT` env var on all services (enables Cloud Logging trace linking)
- `OTEL_EXPORTER_OTLP_ENDPOINT: http://wfp-otel-collector:4318`
- Image tags pointing to Artifact Registry (`europe-west1-docker.pkg.dev/<project>/wfp/<service>:<tag>`)
- `replicaCount: 2` minimum on all stateless services
- Resource requests/limits tuned to actual load test results

## 16.4 CI/CD pipeline additions

1. **Image build + push job**: On merge to `main`, build Docker images and push to Artifact Registry with commit SHA as tag
2. **Helm diff job**: On PR, run `helm diff upgrade` against the dev cluster (read-only) to show what would change
3. **Deploy to dev job**: On merge to `main`, `helm upgrade --install wfp ... --set image.tag=$SHA`
4. **Deploy to prod job**: Manual trigger or tag-based, with approval gate

**Verification checklist for this step:**
- [ ] `terraform validate` passes on all modules
- [ ] `terraform plan` against an empty GCP project produces the expected resource list
- [ ] `helm lint helm/workflow-platform/ -f helm/workflow-platform/values-gcp.yaml` passes
- [ ] ESO ExternalSecrets manifests validated with `kubectl apply --dry-run=server`
- [ ] `helm template` output reviewed for any hardcoded localhost references

---


---

**Plan** — ← [[Step 15 — Local Observability Verification]] · [[Step 17 — GCP Deployment and Acceptance Testing]] →

> [!abstract]- All notes in this set
> [[Active Tasks]]
> [[Project Context]]
> [[Step 01 — Fix Backend Dockerfiles]]
> [[Step 02 — Runtime test custom-fields-service and notification-service]]
> [[Step 03 — Gateway routing test]]
> [[Step 04 — Docker full-stack build and E2E]]
> [[Step 05 — README documentation]]
> [[Step 09 — Documentation User Manual and Admin Manual]]
> [[Step 10 — BPMN Import Export]]
> [[Step 06 — Helm deployment]]
> [[CI Pipeline Fixes]]
> [[Step 07 — Playwright E2E Tests]]
> [[Step 08 — Flowable BPMN Editor Extensions]]
> [[Frontend API Path Bug Fix]]
> [[Step 11 — Comprehensive E2E Testing and Bug Fixes]]
> [[Verification Log]]
> [[Step 12 — Architecture Diagrams and Data Model]]
> [[Step 13 — SDLC Improvements]]
> [[Step 14 — Observability and BDD Acceptance Tests]]
> [[Step 15 — Local Observability Verification]]
> [[Step 17 — GCP Deployment and Acceptance Testing]]
> [[GCP Cost Estimate]]
> [[Step 18 — Release and Rollback Strategy]]
> [[Step 19 — GCP Observability Readiness no deployment]]
> [[Commit Strategy]]
