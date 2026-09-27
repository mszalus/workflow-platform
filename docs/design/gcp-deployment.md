# GCP Deployment

Infrastructure, deployment runbook, cost estimate and release strategy for running the platform on GKE. Moved from `PLAN.md` steps 16 to 18. Open work is tracked in the GitHub milestone "Parked: GCP deployment".

## Step 16: GCP Infrastructure — Terraform + Helm Preparation — CODE DONE (2026-04-09), verification checklist open

**Goal:** Create all GCP infrastructure as code so the platform can be deployed to GCP with a single `terraform apply` + `helm install`. No deployment happens in this step — only code is written and reviewed.

### 16.1 GCP APIs to enable (one-time, per project)

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

### 16.2 Terraform structure to create

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
| Secret Manager | `google_secret_manager_secret` | DB password, RabbitMQ password, Keycloak admin, JWT secret |
| Workload Identity | `google_service_account` + `google_iam_binding` | Pods write to Cloud Trace + Cloud Logging without key files |
| VPC | `google_compute_network` | Private cluster, no public node IPs |
| Cloud NAT | `google_compute_router_nat` | Outbound internet for pods (pull images, reach Keycloak) |
| Managed cert | `google_compute_managed_ssl_certificate` | TLS for the gateway ingress |

### 16.3 Helm changes required before GCP deployment

**a) External Secrets Operator (ESO)**

Install ESO to bridge Secret Manager → K8s Secrets. Add `ExternalSecret` CRDs for:
- `wfp-db-credentials` (DB username/password)
- `wfp-rabbitmq-credentials`
- `wfp-keycloak-admin`

Services reference these as `envFrom.secretRef` instead of plaintext env vars.

**b) cert-manager + Ingress**

Install `cert-manager` (via Helm) with `ClusterIssuer` pointing to Let's Encrypt (or Google CA). Update gateway Helm chart to add:
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

### 16.4 CI/CD pipeline additions

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

## Step 17: GCP Deployment and Acceptance Testing — NOT STARTED (parked; the 2026-03-22 GCP run was a single-VM docker-compose deploy via `deploy/gcp/`, not GKE)

**Goal:** Deploy the full platform to GCP, run the BDD acceptance tests against it, and verify observability (Cloud Logging, Cloud Trace, Cloud Monitoring).

### 17.1 One-time infrastructure bootstrap

```bash
# Authenticate
gcloud auth application-default login
gcloud config set project YOUR_PROJECT_ID

# Create GCS bucket for state (one-time)
gsutil mb -l europe-west1 gs://YOUR_PROJECT_ID-tfstate
gsutil versioning set on gs://YOUR_PROJECT_ID-tfstate

# Copy and fill in dev tfvars
cp terraform/environments/dev/terraform.tfvars.example terraform/terraform.tfvars
# Edit terraform/terraform.tfvars with real values

# Provision infrastructure (~10 min for full GKE + SQL)
cd terraform
terraform init -backend-config=environments/dev/backend.tf
terraform apply

# Configure kubectl
gcloud container clusters get-credentials wfp-dev \
  --region europe-west1 --project YOUR_PROJECT_ID

# Verify nodes are ready
kubectl get nodes
```

### 17.2 Build and push images to Artifact Registry

```bash
# Authenticate Docker to Artifact Registry
gcloud auth configure-docker europe-west1-docker.pkg.dev

# Build and push (or use CI — see Step 16.4)
REGISTRY=europe-west1-docker.pkg.dev/YOUR_PROJECT/wfp
TAG=$(git rev-parse --short HEAD)

for svc in gateway workflow-service custom-fields-service notification-service audit-service; do
  docker build -f services/$svc/Dockerfile -t $REGISTRY/$svc:$TAG .
  docker push $REGISTRY/$svc:$TAG
done
```

### 17.3 Install prerequisites

```bash
# cert-manager
helm install cert-manager jetstack/cert-manager \
  --namespace cert-manager --create-namespace \
  --set crds.enabled=true

# External Secrets Operator
helm install external-secrets external-secrets/external-secrets \
  --namespace external-secrets --create-namespace

# Create ClusterSecretStore pointing at Secret Manager
kubectl apply -f helm/cluster-secret-store.yaml
```

### 17.4 Deploy the platform

```bash
# Update Helm dependencies (Bitnami PostgreSQL, RabbitMQ, Keycloak sub-charts)
helm dependency update helm/workflow-platform/

# Deploy (first time)
helm install wfp helm/workflow-platform/ \
  -f helm/workflow-platform/values-gcp.yaml \
  --set-string "workflow-service.image.tag=$TAG" \
  --set-string "custom-fields-service.image.tag=$TAG" \
  --set-string "notification-service.image.tag=$TAG" \
  --set-string "audit-service.image.tag=$TAG" \
  --set-string "gateway.image.tag=$TAG" \
  --namespace wfp --create-namespace \
  --timeout 10m --wait

# Verify all pods are Running
kubectl get pods -n wfp
```

### 17.5 Run BDD acceptance tests against GCP

The existing BDD test suite runs against configurable endpoints. Point it at GCP:

```bash
JAVA_HOME='C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.4\jbr' \
  ./gradlew :tests:bdd-acceptance:test --no-daemon \
  -DGATEWAY_URL=https://api.yourdomain.com \
  -DKEYCLOAK_URL=https://auth.yourdomain.com
```

This requires updating `ApiClient.java` to read `GATEWAY_URL` and `KEYCLOAK_URL` from system properties (currently hardcoded to `http://localhost:9080`). That's a 2-line change.

Expected result: all 20 scenarios pass against GCP.

### 17.6 Verify observability on GCP

**Cloud Logging:**
1. GCP Console → Logging → Log Explorer
2. Filter: `resource.type="k8s_container" resource.labels.namespace_name="wfp"`
3. Verify JSON structure: `severity`, `message`, `service`, `traceId`, `tenantId`, `userId` fields
4. Click the Cloud Trace link on any log entry with a trace ID → opens the full trace

**Cloud Trace:**
1. GCP Console → Cloud Trace → Trace List
2. Filter by service name — you should see traces from gateway, workflow-service, etc.
3. Click any trace → span waterfall with latency breakdown per service
4. Verify tenant context propagates correctly across spans

**Cloud Monitoring (Google Managed Prometheus):**
1. GCP Console → Monitoring → Metrics Explorer
2. Select metric `prometheus.googleapis.com/http_server_requests_seconds_count/counter`
3. Filter by `application` label — one line per service
4. Create dashboards for RED metrics (Rate, Errors, Duration) per service

### 17.7 Teardown (after testing)

```bash
# Remove application
helm uninstall wfp --namespace wfp

# Destroy infrastructure (saves ~$5-8/day on dev)
cd terraform/environments/dev
terraform destroy -var-file=dev.tfvars
```

---

## GCP Cost Estimate

All prices approximate, us-central1 / europe-west1 regions, on-demand pricing (2026).

### Dev / Staging environment (minimal, one-shot testing)

| Resource | Spec | Monthly cost |
|---|---|---|
| GKE cluster management fee | 1 cluster (waived for first cluster per billing account) | $0–$73 |
| GKE nodes | 2 × e2-standard-2 (2 vCPU, 8 GB), Spot/preemptible | ~$25 |
| Cloud SQL | PostgreSQL 16, `db-f1-micro` (1 vCPU, 614 MB), no HA | ~$7 |
| Artifact Registry | ~5 GB image storage | ~$0.50 |
| Cloud Load Balancer | 1 L7 HTTP(S) LB | ~$18 |
| Cloud DNS | 1 managed zone | ~$0.50 |
| Cloud Logging | First 50 GB/month free | ~$0 |
| Cloud Trace | First 2.5M spans/month free | ~$0 |
| Cloud Monitoring | Free for GKE metrics | ~$0 |
| Network egress | ~10 GB/month | ~$1 |
| **Total (with free GKE mgmt)** | | **~$52/month** |
| **Total (without free tier)** | | **~$125/month** |

> **Note:** GCP gives new accounts $300 free credit (~3–6 months of dev environment).

### Production environment (minimal, 2 replicas, HA)

| Resource | Spec | Monthly cost |
|---|---|---|
| GKE cluster management | 1 cluster | ~$73 |
| GKE nodes | 3 × n2-standard-4 (4 vCPU, 16 GB), regular | ~$360 |
| Cloud SQL | PostgreSQL 16, `db-n1-standard-2` (2 vCPU, 7.5 GB), HA | ~$185 |
| Artifact Registry | ~20 GB | ~$0.40 |
| Cloud Load Balancer | 1 L7 HTTPS LB | ~$18 |
| Cloud Armor (WAF) | Basic tier | ~$5 |
| Cloud DNS | 1 zone | ~$0.50 |
| Cloud Logging | ~100 GB/month at scale | ~$25 |
| Cloud Trace | ~50M spans at moderate traffic | ~$25 |
| Cloud Monitoring | Custom metrics beyond free tier | ~$10 |
| Network egress | ~50 GB/month | ~$5 |
| **Total** | | **~$707/month** |

### Cost reduction levers

| Action | Saving |
|---|---|
| Use GKE Autopilot instead of Standard (for lower traffic) | 30–40% on node cost |
| Spot/preemptible nodes for non-prod workloads | 60–80% on node cost |
| Committed Use Discounts (1-year) | 37% on compute |
| Cloud SQL shared-core (`f1-micro`) in non-prod | Saves ~$175/month vs `n1-standard-2` |
| Scale down non-prod overnight (node pool min=0) | Saves ~60% on node cost |
| Use Cloud Run instead of GKE for stateless services | Pay only for requests, near-zero idle cost |

---

---

## Step 18: Release and Rollback Strategy — PARTIAL (rolling-update settings in Helm charts; canary and rollback runbook not exercised)

**Goal:** Define a safe, repeatable release process using Kubernetes native features (rolling updates, Helm revisions, replica-weighted canary) — no external tooling required.

### 18.1 Release strategy: rolling update (default)

All Helm charts are configured with `RollingUpdate` strategy (K8s default). A new release proceeds as:

```bash
# 1. Build and push new image to Artifact Registry
TAG=$(git rev-parse --short HEAD)
REGISTRY=europe-west1-docker.pkg.dev/YOUR_PROJECT/wfp-prod

docker build -f services/workflow-service/Dockerfile -t $REGISTRY/workflow-service:$TAG .
docker push $REGISTRY/workflow-service:$TAG

# 2. Upgrade via Helm (atomically upgrades, keeps old revision)
helm upgrade wfp helm/workflow-platform/ \
  -f helm/workflow-platform/values-gcp.yaml \
  --set-string "workflow-service.image.tag=$TAG" \
  --namespace wfp \
  --timeout 5m \
  --wait \
  --atomic   # rolls back automatically if pods fail to become ready
```

The `--atomic` flag means Helm will roll back to the previous revision if any pod fails readiness within `--timeout`. This prevents a bad release from staying stuck in half-upgraded state.

**Rolling update parameters** (already in chart `values.yaml`):
```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxSurge: 1        # one extra pod created before old one terminates
    maxUnavailable: 0  # no downtime — always at least N healthy pods
```

To add these to every chart's `deployment.yaml`, insert under `spec.strategy`:
```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxSurge: 1
    maxUnavailable: 0
```

### 18.2 Canary release: replica weighting

For higher-risk changes, use replica weighting to send a fraction of traffic to the new version before full rollout. This works with any K8s-native service (no service mesh required).

**Method:** Two Deployments, one Service, shared pod label selector.

```bash
# Step 1: Deploy canary (10% of replicas = 1 of 10 total)
kubectl apply -f - <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: workflow-service-canary
  namespace: wfp
spec:
  replicas: 1
  selector:
    matchLabels:
      app: wfp-workflow-service
      track: canary
  template:
    metadata:
      labels:
        app: wfp-workflow-service
        track: canary
    spec:
      containers:
        - name: workflow-service
          image: europe-west1-docker.pkg.dev/YOUR_PROJECT/wfp-prod/workflow-service:$NEW_TAG
          # ... same ports, env, probes as stable
EOF

# Step 2: Verify canary is healthy
kubectl rollout status deployment/workflow-service-canary -n wfp
kubectl logs -l track=canary -n wfp --tail=50

# Step 3: Check error rate in Cloud Monitoring
# Filter: resource.labels.pod_name =~ ".*-canary-.*"

# Step 4a: Promote — upgrade stable deployment to new tag
helm upgrade wfp helm/workflow-platform/ \
  -f helm/workflow-platform/values-gcp.yaml \
  --set-string "workflow-service.image.tag=$NEW_TAG" \
  --namespace wfp --wait --atomic

# Then delete the canary
kubectl delete deployment workflow-service-canary -n wfp

# Step 4b: Abort — delete canary if issues found
kubectl delete deployment workflow-service-canary -n wfp
# Stable deployment was never touched — no rollback needed
```

**Traffic split math:** The K8s Service selects all pods with `app: wfp-workflow-service` regardless of `track` label. With 9 stable replicas + 1 canary = 10% canary traffic. Adjust canary `replicas` for finer control.

### 18.3 Rollback procedure

#### Option A: Helm rollback (recommended for Helm-managed releases)

```bash
# List all revisions
helm history wfp --namespace wfp

# Rollback to previous revision (most common case)
helm rollback wfp --namespace wfp --wait

# Rollback to a specific revision
helm rollback wfp 3 --namespace wfp --wait
```

Helm rollback re-applies the previous `values.yaml` + templates, including image tags. GKE triggers a new rolling update back to the previous pod spec. No data migrations are reversed — database is NOT rolled back.

#### Option B: kubectl rollout (for hotfixes without Helm)

```bash
# See revision history for a deployment
kubectl rollout history deployment/wfp-workflow-service -n wfp

# Undo last rollout
kubectl rollout undo deployment/wfp-workflow-service -n wfp

# Undo to a specific revision
kubectl rollout undo deployment/wfp-workflow-service --to-revision=2 -n wfp

# Monitor rollback
kubectl rollout status deployment/wfp-workflow-service -n wfp
```

**Important:** `kubectl rollout undo` is bypassed by the next `helm upgrade` — use only for emergency hotfixes, then reconcile with Helm.

### 18.4 Database migration handling

Flyway runs at application startup. Since K8s rolling updates overlap old and new pods, migrations must be:

1. **Backwards compatible** — new schema changes must not break old pods still running
   - Add columns as nullable or with defaults (never drop columns in the same release)
   - Two-phase deploy: add column (release N) → backfill (release N) → make NOT NULL (release N+1)

2. **Idempotent** — Flyway `repair-on-migrate: true` (already configured) handles checksum mismatches

3. **Never rename or drop in a single release** — always expand-then-contract over two releases

**Migration rollback:** Flyway does not support automatic migration rollback. If a bad migration is applied:
- Fix forward: write a new migration that reverses the change
- Emergency: restore from Cloud SQL point-in-time backup (PITR) — enabled for HA/prod environments

### 18.5 Automated rollback triggers

Configure readiness probes tightly (already in charts). Add pod disruption budgets to prevent too many pods going down simultaneously:

```yaml
# Apply to each backend service namespace
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: wfp-workflow-service-pdb
  namespace: wfp
spec:
  minAvailable: 1
  selector:
    matchLabels:
      app: wfp-workflow-service
```

Combined with `--atomic` on `helm upgrade`, this ensures:
- Deployment fails fast if new pods don't pass readiness within timeout
- Helm automatically reverts to the last good revision
- Minimum service availability is maintained throughout

### 18.6 Release checklist

Before each production release:

- [ ] All BDD acceptance tests pass in dev/staging (`./gradlew :tests:bdd-acceptance:test`)
- [ ] New Flyway migrations are backwards compatible (tested against prod-snapshot DB)
- [ ] Helm dry-run shows expected changes: `helm upgrade --dry-run wfp ...`
- [ ] Canary deployed and healthy for ≥15 minutes before promotion
- [ ] Rollback procedure reviewed — know which `helm history` revision to target
- [ ] On-call engineer available for 30 minutes post-promotion
- [ ] Cloud Monitoring error rate alert threshold reviewed

---
