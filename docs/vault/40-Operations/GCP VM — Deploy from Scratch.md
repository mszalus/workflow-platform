---
title: GCP VM — Deploy from Scratch
tags:
  - ops
  - gcp
  - docker
type: runbook
source: deploy/gcp/README.md
---
[[Operations MOC]] › **GCP VM — Deploy from Scratch**

One command sets up everything (registry, images, VM, firewall, scheduler):

```bash
gcloud auth login
gcloud config set project YOUR_PROJECT_ID

cd deploy/gcp
bash setup.sh
```

This takes ~15 minutes. The script will:
1. Enable required GCP APIs
2. Create an Artifact Registry and push all 7 Docker images
3. Create a firewall rule for ports 5173, 5174, 8180, 9080
4. Create an e2-medium VM with Docker installed
5. SCP config files and start all 10 containers
6. Set up a Cloud Scheduler job to auto-stop the VM at midnight UTC

Wait ~5 minutes after the script finishes for Java services to start (first boot creates DB schemas). Then access:

| Service | URL |
|---------|-----|
| [[Admin Portal]] | `http://EXTERNAL_IP:5173` |
| [[User Portal]] | `http://EXTERNAL_IP:5174` |
| [[Security and JWT|Keycloak]] | `http://EXTERNAL_IP:8180` |
| Gateway API | `http://EXTERNAL_IP:9080` |


---

**GCP VM runbook** — ← [[GCP VM — Prerequisites]] · [[GCP VM — Login Credentials]] →

> [!abstract]- All notes in this set
> [[GCP VM — Prerequisites]]
> [[GCP VM — Login Credentials]]
> [[GCP VM — Redeploy After Teardown]]
> [[GCP VM — Cost Control]]
> [[GCP VM — Updating After Code Changes]]
> [[GCP VM — Troubleshooting]]
> [[GCP VM — Key Deployment Notes]]
> [[GCP VM — Architecture]]
> [[GCP VM — File Reference]]
