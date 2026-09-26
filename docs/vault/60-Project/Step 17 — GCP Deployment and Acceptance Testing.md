---
title: Step 17 — GCP Deployment and Acceptance Testing
tags:
  - project
  - plan
  - status/open
type: plan
source: PLAN.md
status: open
---
[[Project MOC]] › **Step 17 — GCP Deployment and Acceptance Testing**

**Goal:** Deploy the full platform to GCP, run the BDD acceptance tests against it, and verify observability (Cloud Logging, Cloud Trace, Cloud Monitoring).

## 17.1 One-time infrastructure bootstrap

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

## 17.2 Build and push images to Artifact Registry

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

## 17.3 Install prerequisites

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

## 17.4 Deploy the platform

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

## 17.5 Run BDD acceptance tests against GCP

The existing BDD test suite runs against configurable endpoints. Point it at GCP:

```bash
JAVA_HOME='C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.4\jbr' \
  ./gradlew :tests:bdd-acceptance:test --no-daemon \
  -DGATEWAY_URL=https://api.yourdomain.com \
  -DKEYCLOAK_URL=https://auth.yourdomain.com
```

This requires updating `ApiClient.java` to read `GATEWAY_URL` and `KEYCLOAK_URL` from system properties (currently hardcoded to `http://localhost:9080`). That's a 2-line change.

Expected result: all 20 scenarios pass against GCP.

## 17.6 Verify observability on GCP

**Cloud Logging:**
1. GCP Console → Logging → Log Explorer
2. Filter: `resource.type="k8s_container" resource.labels.namespace_name="wfp"`
3. Verify JSON structure: `severity`, `message`, `service`, `traceId`, `tenantId`, `userId` fields
4. Click the Cloud Trace link on any log entry with a trace ID → opens the full trace

**Cloud Trace:**
1. GCP Console → Cloud Trace → Trace List
2. Filter by service name — you should see traces from gateway, [[Workflow Service|workflow-service]], etc.
3. Click any trace → span waterfall with latency breakdown per service
4. Verify tenant context propagates correctly across spans

**Cloud Monitoring (Google Managed [[Observability Stack|Prometheus]]):**
1. GCP Console → Monitoring → Metrics Explorer
2. Select metric `prometheus.googleapis.com/http_server_requests_seconds_count/counter`
3. Filter by `application` label — one line per service
4. Create dashboards for RED metrics (Rate, Errors, Duration) per service

## 17.7 Teardown (after testing)

```bash
# Remove application
helm uninstall wfp --namespace wfp

# Destroy infrastructure (saves ~$5-8/day on dev)
cd terraform/environments/dev
terraform destroy -var-file=dev.tfvars
```

---


---

**Plan** — ← [[Step 16 — GCP Infrastructure Terraform Helm Preparation]] · [[GCP Cost Estimate]] →

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
> [[Step 16 — GCP Infrastructure Terraform Helm Preparation]]
> [[GCP Cost Estimate]]
> [[Step 18 — Release and Rollback Strategy]]
> [[Step 19 — GCP Observability Readiness no deployment]]
> [[Step 20 — Work Item Tracker on BPMN]]
> [[Commit Strategy]]
