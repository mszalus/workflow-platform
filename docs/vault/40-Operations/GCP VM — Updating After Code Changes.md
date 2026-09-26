---
title: GCP VM — Updating After Code Changes
tags:
  - ops
  - gcp
  - docker
type: runbook
source: deploy/gcp/README.md
---
[[Operations MOC]] › **GCP VM — Updating After Code Changes**

If you've made code changes and want to update the running deployment:

```bash
# 1. Rebuild and push only the changed images
REGISTRY="us-central1-docker.pkg.dev/YOUR_PROJECT_ID/wfp-images"

# Backend service example:
docker build -t $REGISTRY/workflow-service:latest -f services/workflow-service/Dockerfile .
docker push $REGISTRY/workflow-service:latest

# Frontend example:
docker build -t $REGISTRY/admin-portal:latest -f frontend/apps/admin-portal/Dockerfile .
docker push $REGISTRY/admin-portal:latest

# 2. Pull and restart on the VM
gcloud compute ssh wfp-vm --zone=us-central1-a --command="
  cd /opt/wfp
  sudo docker compose pull
  sudo docker compose up -d
"
```


---

**GCP VM runbook** — ← [[GCP VM — Cost Control]] · [[GCP VM — Troubleshooting]] →

> [!abstract]- All notes in this set
> [[GCP VM — Prerequisites]]
> [[GCP VM — Deploy from Scratch]]
> [[GCP VM — Login Credentials]]
> [[GCP VM — Redeploy After Teardown]]
> [[GCP VM — Cost Control]]
> [[GCP VM — Troubleshooting]]
> [[GCP VM — Key Deployment Notes]]
> [[GCP VM — Architecture]]
> [[GCP VM — File Reference]]
