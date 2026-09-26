---
title: Step 18 — Release and Rollback Strategy
tags:
  - project
  - plan
  - status/partial
type: plan
source: PLAN.md
status: partial
---
[[Project MOC]] › **Step 18 — Release and Rollback Strategy**

**Goal:** Define a safe, repeatable release process using Kubernetes native features (rolling updates, [[Helm and Kubernetes|Helm]] revisions, replica-weighted canary) — no external tooling required.

## 18.1 Release strategy: rolling update (default)

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

## 18.2 Canary release: replica weighting

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

## 18.3 Rollback procedure

### Option A: Helm rollback (recommended for Helm-managed releases)

```bash
# List all revisions
helm history wfp --namespace wfp

# Rollback to previous revision (most common case)
helm rollback wfp --namespace wfp --wait

# Rollback to a specific revision
helm rollback wfp 3 --namespace wfp --wait
```

Helm rollback re-applies the previous `values.yaml` + templates, including image tags. GKE triggers a new rolling update back to the previous pod spec. No data migrations are reversed — database is NOT rolled back.

### Option B: kubectl rollout (for hotfixes without Helm)

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

## 18.4 Database migration handling

Flyway runs at application startup. Since K8s rolling updates overlap old and new pods, migrations must be:

1. **Backwards compatible** — new schema changes must not break old pods still running
   - Add columns as nullable or with defaults (never drop columns in the same release)
   - Two-phase deploy: add column (release N) → backfill (release N) → make NOT NULL (release N+1)

2. **Idempotent** — Flyway `repair-on-migrate: true` (already configured) handles checksum mismatches

3. **Never rename or drop in a single release** — always expand-then-contract over two releases

**Migration rollback:** Flyway does not support automatic migration rollback. If a bad migration is applied:
- Fix forward: write a new migration that reverses the change
- Emergency: restore from Cloud SQL point-in-time backup (PITR) — enabled for HA/prod environments

## 18.5 Automated rollback triggers

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

## 18.6 Release checklist

Before each production release:

- [ ] All BDD acceptance tests pass in dev/staging (`./gradlew :tests:bdd-acceptance:test`)
- [ ] New Flyway migrations are backwards compatible (tested against prod-snapshot DB)
- [ ] Helm dry-run shows expected changes: `helm upgrade --dry-run wfp ...`
- [ ] Canary deployed and healthy for ≥15 minutes before promotion
- [ ] Rollback procedure reviewed — know which `helm history` revision to target
- [ ] On-call engineer available for 30 minutes post-promotion
- [ ] Cloud Monitoring error rate alert threshold reviewed

---


---

**Plan** — ← [[GCP Cost Estimate]] · [[Step 19 — GCP Observability Readiness no deployment]] →

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
> [[Step 17 — GCP Deployment and Acceptance Testing]]
> [[GCP Cost Estimate]]
> [[Step 19 — GCP Observability Readiness no deployment]]
> [[Commit Strategy]]
